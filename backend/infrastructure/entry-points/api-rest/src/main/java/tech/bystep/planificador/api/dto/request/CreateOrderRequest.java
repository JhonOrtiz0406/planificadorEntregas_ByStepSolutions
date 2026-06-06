package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String productName;
    @NotBlank
    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private String description;
    private String photoUrl;
    private List<String> photoUrls;
    @NotNull
    @Future
    private LocalDate deliveryDate;
    private BigDecimal totalPrice;
}
