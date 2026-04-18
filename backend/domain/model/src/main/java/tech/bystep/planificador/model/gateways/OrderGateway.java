package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.Order;
import tech.bystep.planificador.model.ProgressStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderGateway {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Order> findByOrganizationId(UUID organizationId);

    List<Order> findByOrganizationIdOrderByDeliveryDateAsc(UUID organizationId);

    List<Order> findPendingDeliveries(UUID organizationId);

    List<Order> findByDeliveryDateBetween(LocalDate start, LocalDate end);

    List<Order> findByProgressStatusNot(ProgressStatus status);

    void deleteById(UUID id);

    String generateOrderNumber(UUID organizationId);
}
