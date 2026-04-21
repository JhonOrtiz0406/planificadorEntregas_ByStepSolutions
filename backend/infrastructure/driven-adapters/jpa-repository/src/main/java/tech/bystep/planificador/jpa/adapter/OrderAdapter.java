package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.OrderEntity;
import tech.bystep.planificador.jpa.repository.OrderJpaRepository;
import tech.bystep.planificador.model.Order;
import tech.bystep.planificador.model.gateways.OrderGateway;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderAdapter implements OrderGateway {

    private final OrderJpaRepository repository;

    @Override
    public Order save(Order order) {
        return toModel(repository.save(toEntity(order)));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public Optional<Order> findByIdAndOrganizationId(UUID id, UUID organizationId) {
        return repository.findByIdAndOrganizationId(id, organizationId).map(this::toModel);
    }

    @Override
    public List<Order> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdOrderByDeliveryDateAsc(organizationId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<Order> findByOrganizationIdOrderByDeliveryDateAsc(UUID organizationId) {
        return repository.findByOrganizationIdOrderByDeliveryDateAsc(organizationId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<Order> findPendingDeliveries(UUID organizationId) {
        return repository.findPendingDeliveries(organizationId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<Order> findByOrganizationIdAndDeliveryDateBetween(UUID organizationId, LocalDate start, LocalDate end) {
        return repository.findByOrganizationIdAndDeliveryDateBetween(organizationId, start, end)
                .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<Order> findOverdueOrders(LocalDate date) {
        return repository.findOverdueOrders(date).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public String generateOrderNumber(UUID organizationId) {
        long count = repository.countByOrganizationId(organizationId) + 1;
        return String.format("ORD-%06d", count);
    }

    private Order toModel(OrderEntity e) {
        return Order.builder()
                .id(e.getId()).orderNumber(e.getOrderNumber()).productName(e.getProductName())
                .clientName(e.getClientName()).clientPhone(e.getClientPhone()).clientAddress(e.getClientAddress())
                .description(e.getDescription()).photoUrl(e.getPhotoUrl()).deliveryDate(e.getDeliveryDate())
                .progressStatus(e.getProgressStatus()).paymentStatus(e.getPaymentStatus())
                .paymentAmount(e.getPaymentAmount()).totalPrice(e.getTotalPrice())
                .organizationId(e.getOrganizationId()).createdById(e.getCreatedById())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private OrderEntity toEntity(Order m) {
        return OrderEntity.builder()
                .id(m.getId()).orderNumber(m.getOrderNumber()).productName(m.getProductName())
                .clientName(m.getClientName()).clientPhone(m.getClientPhone()).clientAddress(m.getClientAddress())
                .description(m.getDescription()).photoUrl(m.getPhotoUrl()).deliveryDate(m.getDeliveryDate())
                .progressStatus(m.getProgressStatus()).paymentStatus(m.getPaymentStatus())
                .paymentAmount(m.getPaymentAmount()).totalPrice(m.getTotalPrice())
                .organizationId(m.getOrganizationId()).createdById(m.getCreatedById())
                .createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt())
                .build();
    }
}
