package tech.bystep.planificador.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateOrderRequest {
    private String productName;
    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private String description;
    private String photoUrl;
    private List<String> photoUrls;
    private LocalDate deliveryDate;
    private BigDecimal totalPrice;
}
