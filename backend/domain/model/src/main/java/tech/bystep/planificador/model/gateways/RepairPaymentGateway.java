package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.RepairPayment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairPaymentGateway {
    RepairPayment save(RepairPayment payment);
    List<RepairPayment> findByRepairId(UUID repairId);
    Optional<RepairPayment> findById(UUID id);
    void deleteById(UUID id);
    BigDecimal sumAmountByRepairId(UUID repairId);
}
