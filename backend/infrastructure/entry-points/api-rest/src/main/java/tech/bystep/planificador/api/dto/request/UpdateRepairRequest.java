package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateRepairRequest {
    private String clientFirstName;
    private String clientLastName;
    private String clientPhone;
    private String itemDescription;
    private String repairDescription;
    @Positive
    private BigDecimal totalPrice;
    private LocalDate entryDate;
    private LocalDate deliveryDate;
    @Size(max = 3, message = "Máximo 3 fotos por arreglo")
    private List<String> photoUrls;
    /** Checkbox "Notificar al cliente por WhatsApp". null = sí (al crear) / sin cambio (al editar). */
    private Boolean notifyWhatsapp;
}
