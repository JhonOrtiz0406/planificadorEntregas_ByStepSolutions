package tech.bystep.planificador.jpa.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization_notification_settings")
@IdClass(NotificationSettingEntity.Key.class)
public class NotificationSettingEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Id
    @Column(name = "event_key")
    private String eventKey;

    @Column(name = "whatsapp_enabled", nullable = false)
    private boolean whatsappEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private UUID organizationId;
        private String eventKey;
    }
}
