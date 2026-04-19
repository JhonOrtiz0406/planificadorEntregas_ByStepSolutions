package tech.bystep.planificador.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_organizations")
@IdClass(UserOrgEntity.UserOrgId.class)
public class UserOrgEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false)
    private String role;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOrgId implements Serializable {
        private UUID userId;
        private UUID organizationId;
    }
}
