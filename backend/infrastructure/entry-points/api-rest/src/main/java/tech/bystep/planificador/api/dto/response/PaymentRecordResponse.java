package tech.bystep.planificador.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordResponse {
    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
}
