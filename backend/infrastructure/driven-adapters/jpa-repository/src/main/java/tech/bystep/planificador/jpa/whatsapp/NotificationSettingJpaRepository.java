package tech.bystep.planificador.jpa.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationSettingJpaRepository
        extends JpaRepository<NotificationSettingEntity, NotificationSettingEntity.Key> {
    List<NotificationSettingEntity> findByOrganizationId(UUID organizationId);
}
