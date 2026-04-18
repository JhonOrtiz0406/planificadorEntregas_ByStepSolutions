package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.bystep.planificador.jpa.entity.ReminderEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReminderJpaRepository extends JpaRepository<ReminderEntity, UUID> {
    List<ReminderEntity> findByOrderId(UUID orderId);

    @Query("SELECT r FROM ReminderEntity r WHERE r.reminderDate = :date AND r.sent = false")
    List<ReminderEntity> findPendingByDate(@Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM ReminderEntity r WHERE r.orderId = :orderId")
    void deleteByOrderId(@Param("orderId") UUID orderId);
}
