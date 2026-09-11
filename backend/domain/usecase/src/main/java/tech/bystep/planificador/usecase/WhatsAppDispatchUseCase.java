package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.gateways.WhatsAppCloudGateway;
import tech.bystep.planificador.model.gateways.WhatsAppConfigGateway;
import tech.bystep.planificador.model.gateways.WhatsAppMessageGateway;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppConnectionStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessageStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppSendResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envía los mensajes pendientes de la cola. Cada mensaje sale con la configuración
 * (número y token) de SU organización. Si el token de una organización falla, solo esa
 * organización queda en ERROR; las demás siguen enviando normalmente.
 */
@RequiredArgsConstructor
public class WhatsAppDispatchUseCase {

    private static final Logger LOG = Logger.getLogger(WhatsAppDispatchUseCase.class.getName());

    static final int BATCH_SIZE = 50;
    static final int MAX_ROUNDS = 10;
    static final int MAX_ATTEMPTS = 4;
    private static final int STUCK_MINUTES = 10;

    private final WhatsAppMessageGateway messageGateway;
    private final WhatsAppConfigGateway configGateway;
    private final WhatsAppCloudGateway cloudGateway;

    /** @return cantidad de mensajes procesados */
    public int dispatchDue() {
        LocalDateTime now = LocalDateTime.now();
        messageGateway.releaseStuck(now.minusMinutes(STUCK_MINUTES));

        int processed = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            List<WhatsAppMessage> batch = messageGateway.claimDue(LocalDateTime.now(), BATCH_SIZE);
            if (batch.isEmpty()) break;

            // Config cacheada por organización solo durante este lote.
            Map<UUID, Optional<WhatsAppConfig>> configs = new HashMap<>();
            for (WhatsAppMessage message : batch) {
                Optional<WhatsAppConfig> config = configs.computeIfAbsent(
                        message.getOrganizationId(), configGateway::findByOrganizationId);
                process(message, config.orElse(null));
            }
            processed += batch.size();
            if (batch.size() < BATCH_SIZE) break;
        }
        return processed;
    }

    private void process(WhatsAppMessage message, WhatsAppConfig config) {
        LocalDateTime now = LocalDateTime.now();
        try {
            if (config == null || !config.isEnabled()) {
                message.setStatus(WhatsAppMessageStatus.SKIPPED);
                message.setSkipReason("MODULE_DISABLED");
            } else if (config.getStatus() != WhatsAppConnectionStatus.CONNECTED || !config.hasCredentials()) {
                retryOrFail(message, "NOT_CONNECTED",
                        config.getLastError() != null ? config.getLastError() : "WhatsApp no está conectado", now);
            } else {
                WhatsAppSendResult result = cloudGateway.sendTemplate(config, message.getToPhone(),
                        message.getTemplateName(), message.getLanguageCode(), message.getParams());
                if (result.success()) {
                    message.setStatus(WhatsAppMessageStatus.SENT);
                    message.setWamid(result.wamid());
                    message.setSentAt(now);
                    message.setErrorCode(null);
                    message.setErrorMessage(null);
                } else if (result.credentialsError()) {
                    markConfigError(config, result.errorMessage());
                    retryOrFail(message, result.errorCode(), result.errorMessage(), now);
                } else if (result.retryable()) {
                    retryOrFail(message, result.errorCode(), result.errorMessage(), now);
                } else {
                    message.setStatus(WhatsAppMessageStatus.FAILED);
                    message.setErrorCode(result.errorCode());
                    message.setErrorMessage(result.errorMessage());
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error enviando WhatsApp " + message.getId() + ": " + e.getMessage(), e);
            retryOrFail(message, "INTERNAL", e.getMessage(), now);
        }
        message.setUpdatedAt(now);
        messageGateway.save(message);
    }

    private void retryOrFail(WhatsAppMessage message, String code, String error, LocalDateTime now) {
        message.setErrorCode(code);
        message.setErrorMessage(error);
        if (message.getAttempts() >= MAX_ATTEMPTS) {
            message.setStatus(WhatsAppMessageStatus.FAILED);
        } else {
            message.setStatus(WhatsAppMessageStatus.PENDING);
            message.setNextAttemptAt(now.plusMinutes(backoffMinutes(message.getAttempts())));
        }
    }

    static long backoffMinutes(int attempts) {
        return switch (attempts) {
            case 0, 1 -> 1;
            case 2 -> 5;
            default -> 30;
        };
    }

    private void markConfigError(WhatsAppConfig config, String error) {
        try {
            config.setStatus(WhatsAppConnectionStatus.ERROR);
            config.setLastError(error);
            config.setUpdatedAt(LocalDateTime.now());
            configGateway.save(config);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo marcar en ERROR la config de " + config.getOrganizationId(), e);
        }
    }
}
