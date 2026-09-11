package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.gateways.NotificationSettingsGateway;
import tech.bystep.planificador.model.gateways.OrganizationGateway;
import tech.bystep.planificador.model.gateways.WhatsAppCloudGateway;
import tech.bystep.planificador.model.gateways.WhatsAppConfigGateway;
import tech.bystep.planificador.model.gateways.WhatsAppMessageGateway;
import tech.bystep.planificador.model.gateways.WhatsAppTemplateGateway;
import tech.bystep.planificador.model.whatsapp.NotificationEvent;
import tech.bystep.planificador.model.whatsapp.WhatsAppApiException;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppConnectionStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessageStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppPhoneInfo;
import tech.bystep.planificador.model.whatsapp.WhatsAppRemoteTemplate;
import tech.bystep.planificador.model.whatsapp.WhatsAppSendResult;
import tech.bystep.planificador.model.whatsapp.WhatsAppTemplateState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Administración del módulo WhatsApp de UNA organización: credenciales, verificación,
 * plantillas, eventos activos, prueba y métricas. Todas las operaciones reciben el
 * organizationId y solo tocan datos de esa organización.
 */
@RequiredArgsConstructor
public class WhatsAppAdminUseCase {

    private static final int MAX_FOOTER = 60;

    private final OrganizationGateway organizationGateway;
    private final WhatsAppConfigGateway configGateway;
    private final WhatsAppTemplateGateway templateGateway;
    private final NotificationSettingsGateway settingsGateway;
    private final WhatsAppMessageGateway messageGateway;
    private final WhatsAppCloudGateway cloudGateway;

    // ── Vistas ───────────────────────────────────────────────────────────────

    public record EventView(String key, String label, String templateName, boolean enabled,
                            String templateStatus, String rejectionReason) {
    }

    public record Overview(UUID organizationId, String organizationPhone, WhatsAppConfig config,
                           List<EventView> events, Map<String, Long> monthCounts) {
    }

    public Overview getOverview(UUID organizationId) {
        Organization org = requireOrganization(organizationId);
        WhatsAppConfig config = configGateway.findByOrganizationId(organizationId).orElse(null);
        String language = config != null ? config.language() : "es";

        Map<String, Boolean> settings = settingsGateway.findByOrganizationId(organizationId);
        Map<String, WhatsAppTemplateState> templates = new LinkedHashMap<>();
        for (WhatsAppTemplateState t : templateGateway.findByOrganizationId(organizationId)) {
            if (language.equalsIgnoreCase(t.getLanguageCode())) templates.put(t.getTemplateKey(), t);
        }

        List<EventView> events = new ArrayList<>();
        for (NotificationEvent event : NotificationEvent.applicableTo(org.getCategory())) {
            Boolean configured = settings.get(event.name());
            WhatsAppTemplateState t = templates.get(event.name());
            events.add(new EventView(event.name(), event.label(), event.templateName(),
                    configured != null ? configured : event.defaultEnabled(org.getCategory()),
                    t != null ? t.getStatus() : WhatsAppTemplateState.NOT_CREATED,
                    t != null ? t.getRejectionReason() : null));
        }

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new Overview(organizationId, org.getOrganizationPhone(), config, events,
                messageGateway.countByStatusSince(organizationId, monthStart));
    }

    public List<WhatsAppMessage> recentMessages(UUID organizationId, int limit) {
        requireOrganization(organizationId);
        return messageGateway.findRecentByOrganization(organizationId, Math.max(1, Math.min(limit, 100)));
    }

    // ── Credenciales y conexión ──────────────────────────────────────────────

    /**
     * Guarda las credenciales de Meta de la organización y las verifica de inmediato.
     * Si {@code accessToken} viene vacío se conserva el token ya guardado.
     */
    public WhatsAppConfig saveCredentials(UUID organizationId, String wabaId, String phoneNumberId,
                                          String accessToken, String supportContactText) {
        requireOrganization(organizationId);
        String cleanWaba = digitsOnly(wabaId, "WABA ID");
        String cleanPhoneId = digitsOnly(phoneNumberId, "Phone Number ID");
        if (configGateway.isPhoneNumberIdUsedByAnotherOrganization(cleanPhoneId, organizationId)) {
            throw new IllegalArgumentException("Ese número de WhatsApp ya está conectado a otra organización");
        }
        String footer = supportContactText == null ? null : supportContactText.trim();
        if (footer != null && footer.length() > MAX_FOOTER) {
            throw new IllegalArgumentException("El texto de contacto admite máximo " + MAX_FOOTER + " caracteres");
        }

        WhatsAppConfig config = configGateway.findByOrganizationId(organizationId)
                .orElseGet(() -> newConfig(organizationId));
        boolean phoneChanged = config.getPhoneNumberId() != null && !config.getPhoneNumberId().equals(cleanPhoneId);
        String token = accessToken == null ? "" : accessToken.trim();
        if (!token.isEmpty()) {
            config.setAccessToken(token);
        } else if (phoneChanged || config.getAccessToken() == null || config.getAccessToken().isBlank()) {
            throw new IllegalArgumentException("El access token es obligatorio");
        }

        config.setWabaId(cleanWaba);
        config.setPhoneNumberId(cleanPhoneId);
        config.setSupportContactText(footer == null || footer.isEmpty() ? null : footer);
        config.setStatus(WhatsAppConnectionStatus.PENDING);
        config.setLastError(null);
        config.setUpdatedAt(LocalDateTime.now());
        configGateway.save(config);
        return verify(organizationId);
    }

    public WhatsAppConfig verify(UUID organizationId) {
        Organization org = requireOrganization(organizationId);
        WhatsAppConfig config = requireConfig(organizationId);
        if (!config.hasCredentials()) {
            throw new IllegalStateException("Primero registra las credenciales de Meta");
        }

        WhatsAppPhoneInfo info = cloudGateway.getPhoneInfo(config);
        LocalDateTime now = LocalDateTime.now();
        if (info.success()) {
            config.setStatus(WhatsAppConnectionStatus.CONNECTED);
            config.setDisplayPhoneNumber(info.displayPhoneNumber());
            config.setVerifiedName(info.verifiedName());
            config.setQualityRating(info.qualityRating());
            config.setLastVerifiedAt(now);
            config.setLastError(phoneMismatchWarning(org, info.displayPhoneNumber()));
        } else {
            config.setStatus(WhatsAppConnectionStatus.ERROR);
            config.setLastError(info.errorMessage());
        }
        config.setUpdatedAt(now);
        return configGateway.save(config);
    }

    /** Activa/desactiva el módulo para la organización (lo que se cobra como add-on). */
    public WhatsAppConfig setEnabled(UUID organizationId, boolean enabled) {
        requireOrganization(organizationId);
        WhatsAppConfig config = configGateway.findByOrganizationId(organizationId)
                .orElseGet(() -> newConfig(organizationId));
        config.setEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        return configGateway.save(config);
    }

    // ── Plantillas ───────────────────────────────────────────────────────────

    /** Crea en el WABA de la organización las plantillas del catálogo que falten y refresca estados. */
    public List<WhatsAppTemplateState> syncTemplates(UUID organizationId) {
        Organization org = requireOrganization(organizationId);
        WhatsAppConfig config = requireConnected(organizationId);
        String language = config.language();

        Map<String, WhatsAppRemoteTemplate> remote = remoteByName(config, language);
        Map<String, String> creationErrors = new LinkedHashMap<>();
        for (NotificationEvent event : NotificationEvent.applicableTo(org.getCategory())) {
            if (remote.containsKey(event.templateName())) continue;
            try {
                WhatsAppRemoteTemplate created = cloudGateway.createTemplate(config, event, config.getSupportContactText());
                if (created != null) remote.put(event.templateName(), created);
            } catch (WhatsAppApiException e) {
                creationErrors.put(event.name(), e.getMessage());
            }
        }
        // Estado definitivo según Meta (incluye las recién creadas).
        Map<String, WhatsAppRemoteTemplate> refreshed = remoteByName(config, language);
        remote.putAll(refreshed);
        return storeStates(organizationId, org.getCategory(), language, remote, creationErrors);
    }

    public List<WhatsAppTemplateState> refreshTemplates(UUID organizationId) {
        Organization org = requireOrganization(organizationId);
        WhatsAppConfig config = requireConnected(organizationId);
        String language = config.language();
        return storeStates(organizationId, org.getCategory(), language, remoteByName(config, language), Map.of());
    }

    private List<WhatsAppTemplateState> storeStates(UUID organizationId, String category, String language,
                                                    Map<String, WhatsAppRemoteTemplate> remote,
                                                    Map<String, String> creationErrors) {
        List<WhatsAppTemplateState> result = new ArrayList<>();
        for (NotificationEvent event : NotificationEvent.applicableTo(category)) {
            WhatsAppTemplateState state = templateGateway.find(organizationId, event.name(), language)
                    .orElseGet(() -> WhatsAppTemplateState.builder()
                            .organizationId(organizationId)
                            .templateKey(event.name())
                            .languageCode(language)
                            .build());
            state.setTemplateName(event.templateName());
            WhatsAppRemoteTemplate r = remote.get(event.templateName());
            if (r != null) {
                state.setMetaTemplateId(r.id());
                state.setStatus(r.status() != null ? r.status().toUpperCase() : "PENDING");
                state.setRejectionReason(r.rejectedReason() == null || "NONE".equalsIgnoreCase(r.rejectedReason())
                        ? null : r.rejectedReason());
            } else if (creationErrors.containsKey(event.name())) {
                state.setStatus("ERROR");
                state.setRejectionReason(creationErrors.get(event.name()));
            } else {
                state.setStatus(WhatsAppTemplateState.NOT_CREATED);
                state.setRejectionReason(null);
            }
            state.setUpdatedAt(LocalDateTime.now());
            result.add(templateGateway.save(state));
        }
        return result;
    }

    private Map<String, WhatsAppRemoteTemplate> remoteByName(WhatsAppConfig config, String language) {
        Map<String, WhatsAppRemoteTemplate> map = new LinkedHashMap<>();
        for (WhatsAppRemoteTemplate t : cloudGateway.listTemplates(config)) {
            if (t.name() != null && language.equalsIgnoreCase(t.language())) map.put(t.name(), t);
        }
        return map;
    }

    // ── Eventos ──────────────────────────────────────────────────────────────

    public void updateEventSettings(UUID organizationId, Map<String, Boolean> events) {
        Organization org = requireOrganization(organizationId);
        if (events == null) return;
        for (NotificationEvent event : NotificationEvent.applicableTo(org.getCategory())) {
            Boolean value = events.get(event.name());
            if (value != null) settingsGateway.save(organizationId, event.name(), value);
        }
    }

    // ── Prueba ───────────────────────────────────────────────────────────────

    /** Envía de inmediato un mensaje de prueba (datos de ejemplo) al celular indicado. */
    public WhatsAppSendResult sendTest(UUID organizationId, String phone) {
        Organization org = requireOrganization(organizationId);
        WhatsAppConfig config = requireConnected(organizationId);
        String to = NotificationText.normalizePhone(phone);
        if (to == null) throw new IllegalArgumentException("Celular inválido");

        NotificationEvent event = pickApprovedEvent(organizationId, org.getCategory(), config.language())
                .orElseThrow(() -> new IllegalStateException(
                        "Aún no hay plantillas aprobadas por Meta. Sincroniza y espera la aprobación."));

        WhatsAppSendResult result = cloudGateway.sendTemplate(config, to, event.templateName(),
                config.language(), event.exampleParams());

        LocalDateTime now = LocalDateTime.now();
        messageGateway.save(WhatsAppMessage.builder()
                .organizationId(organizationId)
                .eventKey("TEST")
                .toPhone(to)
                .templateName(event.templateName())
                .languageCode(config.language())
                .params(event.exampleParams())
                .status(result.success() ? WhatsAppMessageStatus.SENT : WhatsAppMessageStatus.FAILED)
                .wamid(result.wamid())
                .errorCode(result.errorCode())
                .errorMessage(result.errorMessage())
                .attempts(1)
                .createdAt(now)
                .updatedAt(now)
                .sentAt(result.success() ? now : null)
                .build());
        return result;
    }

    private Optional<NotificationEvent> pickApprovedEvent(UUID organizationId, String category, String language) {
        List<NotificationEvent> candidates = new ArrayList<>(NotificationEvent.applicableTo(category));
        candidates.remove(NotificationEvent.ORDER_READY);
        candidates.add(0, NotificationEvent.ORDER_READY);
        for (NotificationEvent event : candidates) {
            boolean approved = templateGateway.find(organizationId, event.name(), language)
                    .map(WhatsAppTemplateState::isApproved).orElse(false);
            if (approved) return Optional.of(event);
        }
        return Optional.empty();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Organization requireOrganization(UUID organizationId) {
        return organizationGateway.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada"));
    }

    private WhatsAppConfig requireConfig(UUID organizationId) {
        return configGateway.findByOrganizationId(organizationId)
                .orElseThrow(() -> new IllegalStateException("La organización no tiene WhatsApp configurado"));
    }

    private WhatsAppConfig requireConnected(UUID organizationId) {
        WhatsAppConfig config = requireConfig(organizationId);
        if (config.getStatus() != WhatsAppConnectionStatus.CONNECTED || !config.hasCredentials()) {
            throw new IllegalStateException("WhatsApp no está conectado. Verifica las credenciales primero.");
        }
        return config;
    }

    private static WhatsAppConfig newConfig(UUID organizationId) {
        LocalDateTime now = LocalDateTime.now();
        return WhatsAppConfig.builder()
                .organizationId(organizationId)
                .enabled(false)
                .status(WhatsAppConnectionStatus.PENDING)
                .languageCode("es")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String digitsOnly(String value, String field) {
        String v = value == null ? "" : value.trim();
        if (!v.matches("\\d{5,30}")) {
            throw new IllegalArgumentException(field + " inválido: debe contener solo números");
        }
        return v;
    }

    private static String phoneMismatchWarning(Organization org, String displayPhone) {
        String expected = NotificationText.normalizePhone(org.getOrganizationPhone());
        String actual = NotificationText.normalizePhone(displayPhone);
        if (expected == null || actual == null || expected.equals(actual)) return null;
        return "Aviso: el número conectado en Meta (" + displayPhone
                + ") no coincide con el número registrado de la organización (" + org.getOrganizationPhone() + ")";
    }
}
