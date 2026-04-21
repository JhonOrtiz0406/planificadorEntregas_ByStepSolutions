package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.Order;

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

    List<Order> findByOrganizationIdAndDeliveryDateBetween(UUID organizationId, LocalDate start, LocalDate end);

    void deleteById(UUID id);

    String generateOrderNumber(UUID organizationId);

    List<Order> findOverdueOrders(LocalDate date);
}
