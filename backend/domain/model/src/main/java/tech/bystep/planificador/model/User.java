package tech.bystep.planificador.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private UUID id;
    private String googleId;
    private String email;
    private String name;
    private String pictureUrl;
    private UserRole role;
    private UUID organizationId;
    private String fcmToken;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
