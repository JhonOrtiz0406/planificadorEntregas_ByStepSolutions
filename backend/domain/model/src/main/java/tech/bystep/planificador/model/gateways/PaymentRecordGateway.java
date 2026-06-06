package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.PaymentRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordGateway {
    PaymentRecord save(PaymentRecord record);
    List<PaymentRecord> findByOrderId(UUID orderId);
    Optional<PaymentRecord> findById(UUID id);
    void deleteById(UUID id);
    BigDecimal sumAmountByOrderId(UUID orderId);
}
