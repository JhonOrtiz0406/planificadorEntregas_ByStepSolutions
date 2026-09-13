package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.gateways.NotificationSettingsGateway;
import tech.bystep.planificador.model.gateways.OrganizationGateway;
import tech.bystep.planificador.model.gateways.WhatsAppConfigGateway;
import tech.bystep.planificador.model.gateways.WhatsAppDispatchTrigger;
import tech.bystep.planificador.model.gateways.WhatsAppMessageGateway;
import tech.bystep.planificador.model.gateways.WhatsAppTemplateGateway;
import tech.bystep.planificador.model.whatsapp.NotificationEvent;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppConnectionStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessageStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto único para notificar al cliente final de UNA organización.
 *
 * <p>No llama a Meta: deja el mensaje en la cola de esa organización y pide el envío
 * inmediato. Así crear un pedido nunca depende de que WhatsApp responda, y un problema
 * con el número de una organización no afecta a ninguna otra.</p>
 *
 * <p>Nunca lanza excepciones hacia el caso de uso que la llama.</p>
 */
@RequiredArgsConstructor
public class ClientNotificationUseCase {

    private static final Logger LOG = Logger.getLogger(ClientNotificationUseCase.class.getName());

    public static final String ENTITY_ORDER = "ORDER";
    public static final String ENTITY_REPAIR = "REPAIR";

    private final OrganizationGateway organizationGateway;
    private final WhatsAppConfigGateway configGateway;
    private final WhatsAppTemplateGateway templateGateway;
    private final NotificationSettingsGateway settingsGateway;
    private final WhatsAppMessageGateway messageGateway;
    private final WhatsAppDispatchTrigger dispatchTrigger;

    /**
     * @param organizationId organización dueña del pedido/arreglo (y del número que envía)
     * @param event          evento del catálogo
     * @param entityType     {@link #ENTITY_ORDER} o {@link #ENTITY_REPAIR}
     * @param entityId       id del pedido/arreglo
     * @param clientPhone    celular del cliente final tal como se capturó
     * @param clientAccepts  checkbox "Notificar por WhatsApp" del pedido/arreglo (null = sí)
     * @param params         parámetros de la plantilla, en orden
     * @param idempotencyKey evita enviar dos veces lo mismo (p. ej. ORDER:{id}:STATUS:READY)
     */
    public void notify(UUID organizationId, NotificationEvent event, String entityType, UUID entityId,
                       String clientPhone, Boolean clientAccepts, List<String> params, String idempotencyKey) {
        try {
            enqueue(organizationId, event, entityType, entityId, clientPhone, clientAccepts, params, idempotencyKey);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo encolar la notificación " + event + " de la organización "
                    + organizationId + ": " + e.getMessage(), e);
        }
    }

    private void enqueue(UUID organizationId, NotificationEvent event, String entityType, UUID entityId,
                         String clientPhone, Boolean clientAccepts, List<String> params, String idempotencyKey) {
        if (organizationId == null || event == null) return;

        // 1. ¿Esta organización tiene el módulo WhatsApp activo? Si no, no hay nada que registrar.
        Optional<WhatsAppConfig> configOpt = configGateway.findByOrganizationId(organizationId);
        if (configOpt.isEmpty() || !configOpt.get().isEnabled()) return;
        WhatsAppConfig config = configOpt.get();

        Organization org = organizationGateway.findById(organizationId).orElse(null);
        if (org == null || !org.isActive()) return;
        if (!event.appliesTo(org.getCategory())) return;

        // 2. ¿La organización quiere notificar este evento?
        Boolean configured = settingsGateway.findByOrganizationId(organizationId).get(event.name());
        boolean eventEnabled = configured != null ? configured : event.defaultEnabled(org.getCategory());
        if (!eventEnabled) return;

        // 3. ¿Ya se envió? (mismo pedido + mismo evento)
        if (idempotencyKey != null && messageGateway.existsByIdempotencyKey(idempotencyKey)) return;

        List<String> cleanParams = params == null ? List.of()
                : params.stream().map(NotificationText::param).toList();
        String phone = NotificationText.normalizePhone(clientPhone);

        String skipReason = null;
        if (Boolean.FALSE.equals(clientAccepts)) {
            skipReason = "CLIENT_NOT_ACCEPTED";
        } else if (phone == null) {
            skipReason = "INVALID_PHONE";
        } else if (config.getStatus() != WhatsAppConnectionStatus.CONNECTED || !config.hasCredentials()) {
            skipReason = "NOT_CONNECTED";
        } else if (cleanParams.size() != event.paramCount()) {
            skipReason = "INVALID_PARAMS";
        } else if (!isTemplateApproved(organizationId, event, config.language())) {
            skipReason = "TEMPLATE_NOT_APPROVED";
        }

        LocalDateTime now = LocalDateTime.now();
        WhatsAppMessage message = WhatsAppMessage.builder()
                .organizationId(organizationId)
                .eventKey(event.name())
                .entityType(entityType)
                .entityId(entityId)
                .toPhone(phone)
                .templateName(event.templateName())
                .languageCode(config.language())
                .params(cleanParams)
                .status(skipReason == null ? WhatsAppMessageStatus.PENDING : WhatsAppMessageStatus.SKIPPED)
                .skipReason(skipReason)
                .attempts(0)
                .nextAttemptAt(now)
                // Solo los mensajes que realmente se van a enviar "consumen" la llave: si se omitió
                // (p. ej. plantilla aún sin aprobar), un cambio de estado posterior sí podrá enviarse.
                .idempotencyKey(skipReason == null ? idempotencyKey : null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        messageGateway.save(message);

        if (skipReason == null) {
            dispatchTrigger.requestDispatch();
        }
    }

    private boolean isTemplateApproved(UUID organizationId, NotificationEvent event, String language) {
        return templateGateway.find(organizationId, event.name(), language)
                .map(t -> t.isApproved())
                .orElse(false);
    }
}
