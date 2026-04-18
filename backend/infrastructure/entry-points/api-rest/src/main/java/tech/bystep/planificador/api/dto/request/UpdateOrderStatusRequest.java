package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.bystep.planificador.model.PaymentStatus;
import tech.bystep.planificador.model.ProgressStatus;

import java.math.BigDecimal;

@Data
public class UpdateOrderStatusRequest {
    private ProgressStatus progressStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal paymentAmount;
}
