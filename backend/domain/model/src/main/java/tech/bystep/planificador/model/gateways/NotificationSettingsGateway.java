package tech.bystep.planificador.model.gateways;

import java.util.Map;
import java.util.UUID;

public interface NotificationSettingsGateway {
    /** event_key → habilitado. Solo trae los eventos que la organización configuró explícitamente. */
    Map<String, Boolean> findByOrganizationId(UUID organizationId);
    void save(UUID organizationId, String eventKey, boolean whatsappEnabled);
}
