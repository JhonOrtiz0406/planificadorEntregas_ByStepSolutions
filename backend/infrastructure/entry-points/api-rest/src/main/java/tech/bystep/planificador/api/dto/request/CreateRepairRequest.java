package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateRepairRequest {
    @NotBlank
    private String clientFirstName;
    @NotBlank
    private String clientLastName;
    @NotBlank
    private String clientPhone;
    @NotBlank
    private String itemDescription;
    @NotBlank
    private String repairDescription;
    @NotNull
    @Positive
    private BigDecimal totalPrice;
    private LocalDate entryDate;
    private LocalDate deliveryDate;
    @Size(max = 3, message = "Máximo 3 fotos por arreglo")
    private List<String> photoUrls;
}
