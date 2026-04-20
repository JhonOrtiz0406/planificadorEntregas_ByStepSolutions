package tech.bystep.planificador.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganization {
    private UUID userId;
    private UUID organizationId;
    private String role;
}
