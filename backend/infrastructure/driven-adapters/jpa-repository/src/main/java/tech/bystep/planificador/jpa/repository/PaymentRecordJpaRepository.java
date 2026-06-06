package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tech.bystep.planificador.jpa.entity.PaymentRecordEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRecordJpaRepository extends JpaRepository<PaymentRecordEntity, UUID> {
    List<PaymentRecordEntity> findByOrderIdOrderByPaymentDateDesc(UUID orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentRecordEntity p WHERE p.orderId = :orderId")
    BigDecimal sumAmountByOrderId(UUID orderId);
}
