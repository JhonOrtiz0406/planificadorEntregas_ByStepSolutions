package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.bystep.planificador.jpa.entity.RepairPaymentEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RepairPaymentJpaRepository extends JpaRepository<RepairPaymentEntity, UUID> {
    List<RepairPaymentEntity> findByRepairIdOrderByPaymentDateDesc(UUID repairId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM RepairPaymentEntity p WHERE p.repairId = :repairId")
    BigDecimal sumAmountByRepairId(UUID repairId);
}
