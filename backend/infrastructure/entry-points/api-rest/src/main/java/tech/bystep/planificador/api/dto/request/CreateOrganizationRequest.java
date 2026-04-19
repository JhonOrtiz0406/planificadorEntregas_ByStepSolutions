package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank
    private String name;
    private String logoUrl;
    @Email
    @NotBlank
    private String adminEmail;
    private String category;
}
