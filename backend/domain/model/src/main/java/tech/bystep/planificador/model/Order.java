package tech.bystep.planificador.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class Order {

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
    private UUID organizationId;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long daysUntilDelivery() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), this.deliveryDate);
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(this.deliveryDate) && this.progressStatus != ProgressStatus.DELIVERED;
    }
}
