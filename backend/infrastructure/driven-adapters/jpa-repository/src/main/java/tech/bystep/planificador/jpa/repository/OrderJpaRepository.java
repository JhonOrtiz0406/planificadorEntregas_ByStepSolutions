package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.bystep.planificador.jpa.entity.OrderEntity;
import tech.bystep.planificador.model.ProgressStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<OrderEntity> findByOrganizationIdOrderByDeliveryDateAsc(UUID organizationId);

    @Query("SELECT o FROM OrderEntity o WHERE o.organizationId = :orgId AND o.progressStatus != 'DELIVERED' ORDER BY o.deliveryDate ASC")
    List<OrderEntity> findPendingDeliveries(@Param("orgId") UUID orgId);

    List<OrderEntity> findByDeliveryDateBetween(LocalDate start, LocalDate end);

    List<OrderEntity> findByProgressStatusNot(ProgressStatus status);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.organizationId = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);
}
