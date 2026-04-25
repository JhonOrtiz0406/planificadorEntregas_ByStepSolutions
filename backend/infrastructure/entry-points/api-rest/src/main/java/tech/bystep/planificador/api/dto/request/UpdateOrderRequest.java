package tech.bystep.planificador.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateOrderRequest {
    private String productName;
    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private String description;
    private String photoUrl;
    private LocalDate deliveryDate;
    private BigDecimal totalPrice;
}
