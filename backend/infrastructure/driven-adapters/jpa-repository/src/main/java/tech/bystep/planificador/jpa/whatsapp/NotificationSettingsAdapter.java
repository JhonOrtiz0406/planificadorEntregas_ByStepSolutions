package tech.bystep.planificador.jpa.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.model.gateways.NotificationSettingsGateway;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationSettingsAdapter implements NotificationSettingsGateway {

    private final NotificationSettingJpaRepository repository;

    @Override
    public Map<String, Boolean> findByOrganizationId(UUID organizationId) {
        Map<String, Boolean> result = new HashMap<>();
        for (NotificationSettingEntity e : repository.findByOrganizationId(organizationId)) {
            result.put(e.getEventKey(), e.isWhatsappEnabled());
        }
        return result;
    }

    @Override
    public void save(UUID organizationId, String eventKey, boolean whatsappEnabled) {
        repository.save(NotificationSettingEntity.builder()
                .organizationId(organizationId)
                .eventKey(eventKey)
                .whatsappEnabled(whatsappEnabled)
                .updatedAt(LocalDateTime.now())
                .build());
    }
}
