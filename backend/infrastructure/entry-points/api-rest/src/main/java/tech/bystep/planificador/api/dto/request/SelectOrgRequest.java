package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SelectOrgRequest {
    @NotBlank
    private String selectionToken;
    @NotNull
    private UUID organizationId;
}
