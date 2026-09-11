package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Se usa para crear (POST, validado) y para actualizar (PUT, parcial: los campos null no cambian).
 */
@Data
public class CreateOrganizationRequest {
    @NotBlank
    private String name;
    private String logoUrl;
    @Email
    @NotBlank
    private String adminEmail;
    private String category;

    /** Datos del administrador (responsable) de la organización. */
    @NotBlank(message = "Los nombres del administrador son obligatorios")
    @Size(max = 100)
    private String adminFirstName;

    @NotBlank(message = "Los apellidos del administrador son obligatorios")
    @Size(max = 100)
    private String adminLastName;

    @NotBlank(message = "El celular personal del administrador es obligatorio")
    @Size(max = 20)
    private String adminPhone;

    /** Número principal de la organización: el que se registra en Meta para WhatsApp. Opcional. */
    @Size(max = 20)
    private String organizationPhone;
}
