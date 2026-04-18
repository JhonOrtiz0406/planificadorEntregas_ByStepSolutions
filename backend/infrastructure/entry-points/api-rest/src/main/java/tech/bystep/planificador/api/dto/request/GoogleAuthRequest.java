package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {
    @NotBlank
    private String idToken;
    private String fcmToken;
    private String invitationToken;
}
