package tech.bystep.planificador.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.bystep.planificador.model.PaymentStatus;
import tech.bystep.planificador.model.ProgressStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private String productName;
    private String clientName;
    private String clientPhone;
    private String clientAddress;
    private String description;
    private String photoUrl;
    private List<String> photoUrls = new ArrayList<>();
    private LocalDate deliveryDate;
    private ProgressStatus progressStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal paymentAmount;
    private BigDecimal totalPrice;
    private BigDecimal balanceDue;
    private UUID organizationId;
    private long daysUntilDelivery;
    private boolean overdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
