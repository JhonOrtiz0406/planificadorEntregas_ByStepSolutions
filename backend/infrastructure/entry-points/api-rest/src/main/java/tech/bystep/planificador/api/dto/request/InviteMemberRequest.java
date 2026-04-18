package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.bystep.planificador.model.UserRole;

@Data
public class InviteMemberRequest {
    @Email
    @NotBlank
    private String email;
    @NotNull
    private UserRole role;
}
