package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactSupportRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{L}\\s'.\\-]+$", message = "Name contains invalid characters")
    private String name;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^[+\\d\\s\\-().]+$", message = "Phone contains invalid characters")
    private String phone;

    @Size(max = 150)
    private String organizationName;

    @Size(max = 1000)
    private String message;
}
