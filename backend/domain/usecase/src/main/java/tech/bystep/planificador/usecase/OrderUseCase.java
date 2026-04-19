package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.*;
import tech.bystep.planificador.model.gateways.OrderGateway;
import tech.bystep.planificador.model.gateways.ReminderGateway;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderUseCase {

    private static final int[] REMINDER_DAYS = {5, 3, 1, 0};

    private final OrderGateway orderGateway;
    private final ReminderGateway reminderGateway;

    public Order create(Order order) {
        order.setOrderNumber(orderGateway.generateOrderNumber(order.getOrganizationId()));
        order.setProgressStatus(ProgressStatus.NOT_STARTED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        if (order.getPaymentAmount() == null) order.setPaymentAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderGateway.save(order);
        createReminders(saved);
        return saved;
    }

    public Order update(UUID id, UUID organizationId, Order updates) {
        Order existing = orderGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        if (updates.getProductName() != null) existing.setProductName(updates.getProductName());
        if (updates.getClientName() != null) existing.setClientName(updates.getClientName());
        if (updates.getClientPhone() != null) existing.setClientPhone(updates.getClientPhone());
        if (updates.getClientAddress() != null) existing.setClientAddress(updates.getClientAddress());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getPhotoUrl() != null) existing.setPhotoUrl(updates.getPhotoUrl());
        if (updates.getTotalPrice() != null) existing.setTotalPrice(updates.getTotalPrice());

        if (updates.getDeliveryDate() != null && !updates.getDeliveryDate().equals(existing.getDeliveryDate())) {
            existing.setDeliveryDate(updates.getDeliveryDate());
            reminderGateway.deleteByOrderId(id);
            createReminders(existing);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return orderGateway.save(existing);
    }

    public Order updateProgressStatus(UUID id, UUID organizationId, ProgressStatus status) {
        Order order = orderGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setProgressStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderGateway.save(order);
    }

    public Order updatePaymentStatus(UUID id, UUID organizationId, PaymentStatus status, BigDecimal amount) {
        Order order = orderGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setPaymentStatus(status);
        if (amount != null) order.setPaymentAmount(amount);
        order.setUpdatedAt(LocalDateTime.now());
        return orderGateway.save(order);
    }

    public Optional<Order> findById(UUID id, UUID organizationId) {
        return orderGateway.findByIdAndOrganizationId(id, organizationId);
    }

    public List<Order> findAllByOrganization(UUID organizationId) {
        return orderGateway.findByOrganizationIdOrderByDeliveryDateAsc(organizationId);
    }

    public List<Order> findPendingDeliveries(UUID organizationId) {
        return orderGateway.findPendingDeliveries(organizationId);
    }

    public List<Order> findByDateRange(UUID organizationId, LocalDate start, LocalDate end) {
        return orderGateway.findByOrganizationIdAndDeliveryDateBetween(organizationId, start, end);
    }

    public void delete(UUID id, UUID organizationId) {
        orderGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        reminderGateway.deleteByOrderId(id);
        orderGateway.deleteById(id);
    }

    private void createReminders(Order order) {
        List<Reminder> reminders = new ArrayList<>();
        for (int daysBefore : REMINDER_DAYS) {
            LocalDate reminderDate = order.getDeliveryDate().minusDays(daysBefore);
            if (!reminderDate.isBefore(LocalDate.now())) {
                reminders.add(Reminder.builder()
                        .orderId(order.getId())
                        .organizationId(order.getOrganizationId())
                        .reminderDate(reminderDate)
                        .daysBefore(daysBefore)
                        .sent(false)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }
        reminders.forEach(reminderGateway::save);
    }
}
