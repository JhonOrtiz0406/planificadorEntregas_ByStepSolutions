package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.*;
import tech.bystep.planificador.model.gateways.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OrderUseCase {

    private static final int[] REMINDER_DAYS = {5, 3, 1, 0};
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d/MM/yyyy");

    private final OrderGateway orderGateway;
    private final ReminderGateway reminderGateway;
    private final WhatsAppGateway whatsAppGateway;
    private final NotificationGateway notificationGateway;
    private final UserGateway userGateway;

    public Order create(Order order) {
        order.setOrderNumber(orderGateway.generateOrderNumber(order.getOrganizationId()));
        order.setProgressStatus(ProgressStatus.NOT_STARTED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        if (order.getPaymentAmount() == null) order.setPaymentAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderGateway.save(order);
        createReminders(saved);
        notifyClientOrderCreated(saved);
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

        boolean dateChanged = updates.getDeliveryDate() != null
                && !updates.getDeliveryDate().equals(existing.getDeliveryDate());

        if (dateChanged) {
            LocalDate newDate = updates.getDeliveryDate();
            existing.setDeliveryDate(newDate);
            reminderGateway.deleteByOrderId(id);
            createReminders(existing);
            notifyClientDateChanged(existing, newDate);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return orderGateway.save(existing);
    }

    public Order updateProgressStatus(UUID id, UUID organizationId, ProgressStatus status) {
        Order order = orderGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        order.setProgressStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderGateway.save(order);
        notifyStatusChange(saved, status);
        return saved;
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

    // ── WhatsApp — client notifications ────────────────────────────────────

    private void notifyClientOrderCreated(Order order) {
        String msg = String.format(
                "Hola %s, su pedido *%s* ha sido registrado correctamente. " +
                "Fecha estimada de entrega: *%s*. Le notificaremos sobre el estado. " +
                "N\u00famero de pedido: #%s",
                order.getClientName(), order.getProductName(),
                order.getDeliveryDate().format(DATE_FMT), order.getOrderNumber());
        sendWhatsApp(order.getClientPhone(), msg);
    }

    private void notifyClientDateChanged(Order order, LocalDate newDate) {
        String msg = String.format(
                "Hola %s, la fecha de entrega de su pedido *%s* ha cambiado. " +
                "Nueva fecha estimada: *%s*. N\u00famero de pedido: #%s",
                order.getClientName(), order.getProductName(),
                newDate.format(DATE_FMT), order.getOrderNumber());
        sendWhatsApp(order.getClientPhone(), msg);
    }

    private void notifyStatusChange(Order order, ProgressStatus status) {
        switch (status) {
            case READY_TO_DELIVER -> {
                String msg = String.format(
                        "Hola %s, su pedido *%s* ya est\u00e1 listo y ser\u00e1 enviado pronto. " +
                        "N\u00famero de pedido: #%s",
                        order.getClientName(), order.getProductName(), order.getOrderNumber());
                sendWhatsApp(order.getClientPhone(), msg);
                notifyDeliveryWorkers(order);
            }
            case IN_PREPARATION -> {
                String msg = String.format(
                        "Hola %s, su pedido *%s* est\u00e1 en preparaci\u00f3n. " +
                        "Pronto estar\u00e1 listo. N\u00famero de pedido: #%s",
                        order.getClientName(), order.getProductName(), order.getOrderNumber());
                sendWhatsApp(order.getClientPhone(), msg);
            }
            case DELIVERED -> {
                String msg = String.format(
                        "Hola %s, su pedido *%s* ha sido entregado exitosamente. " +
                        "\u00a1Gracias por su confianza! N\u00famero de pedido: #%s",
                        order.getClientName(), order.getProductName(), order.getOrderNumber());
                sendWhatsApp(order.getClientPhone(), msg);
            }
            default -> { /* otros estados: sin notificación al cliente */ }
        }
    }

    // ── FCM — delivery worker notification on PENDING_DELIVERY ─────────────

    private void notifyDeliveryWorkers(Order order) {
        List<String> tokens = userGateway.findByOrganizationId(order.getOrganizationId()).stream()
                .filter(u -> u.getRole() == UserRole.ORG_DELIVERY)
                .filter(u -> u.getFcmToken() != null && !u.getFcmToken().isBlank())
                .map(User::getFcmToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) return;

        String title = "Nuevo pedido asignado";
        String body = String.format("Pedido #%s - %s para %s. Direcci\u00f3n: %s",
                order.getOrderNumber(), order.getProductName(),
                order.getClientName(), order.getClientAddress());

        Map<String, String> data = Map.of(
                "orderId", order.getId().toString(),
                "orderNumber", order.getOrderNumber(),
                "type", "NEW_DELIVERY_ASSIGNED"
        );

        notificationGateway.sendToMultipleTokens(tokens, title, body, data);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void sendWhatsApp(String phone, String message) {
        if (phone != null && !phone.isBlank()) {
            whatsAppGateway.sendMessage(phone, message);
        }
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
