package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.*;
import tech.bystep.planificador.model.gateways.*;
import tech.bystep.planificador.model.whatsapp.NotificationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OrderUseCase {

    private static final int[] REMINDER_DAYS = {5, 3, 1, 0};

    private final OrderGateway orderGateway;
    private final ReminderGateway reminderGateway;
    private final ClientNotificationUseCase clientNotifications;
    private final NotificationGateway notificationGateway;
    private final UserGateway userGateway;
    private final tech.bystep.planificador.model.gateways.PaymentRecordGateway paymentRecordGateway;
    private final tech.bystep.planificador.model.gateways.StorageGateway storageGateway;

    public Order create(Order order) {
        order.setOrderNumber(orderGateway.generateOrderNumber(order.getOrganizationId()));
        order.setProgressStatus(ProgressStatus.NOT_STARTED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        if (order.getPaymentAmount() == null) order.setPaymentAmount(BigDecimal.ZERO);
        if (order.getNotifyWhatsapp() == null) order.setNotifyWhatsapp(true);
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
        if (updates.getNotifyWhatsapp() != null) existing.setNotifyWhatsapp(updates.getNotifyWhatsapp());

        boolean dateChanged = updates.getDeliveryDate() != null
                && !updates.getDeliveryDate().equals(existing.getDeliveryDate());

        if (dateChanged) {
            LocalDate newDate = updates.getDeliveryDate();
            existing.setDeliveryDate(newDate);
            reminderGateway.deleteByOrderId(id);
            createReminders(existing);
            notifyClientDateChanged(existing, newDate);
        }

        if (updates.getPhotoUrls() != null) existing.setPhotoUrls(updates.getPhotoUrls());
        existing.setUpdatedAt(LocalDateTime.now());
        return orderGateway.save(existing);
    }

    public Order removePhoto(UUID id, UUID organizationId, String photoUrl) {
        Order order = orderGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        List<String> photos = order.getPhotoUrls() != null
                ? new ArrayList<>(order.getPhotoUrls()) : new ArrayList<>();
        boolean belongsToOrder = photos.remove(photoUrl);
        if (photoUrl != null && photoUrl.equals(order.getPhotoUrl())) {
            order.setPhotoUrl(null);
            belongsToOrder = true;
        }
        if (!belongsToOrder) {
            // Aislamiento: nunca borrar archivos que no pertenecen a este pedido (ni a esta organización).
            throw new IllegalArgumentException("La foto no pertenece a este pedido");
        }
        order.setPhotoUrls(photos);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderGateway.save(order);
        storageGateway.deleteFile(photoUrl);
        return saved;
    }

    public tech.bystep.planificador.model.PaymentRecord addPaymentRecord(
            UUID orderId, UUID organizationId,
            java.math.BigDecimal amount, LocalDate paymentDate,
            String paymentMethod, String notes) {
        Order order = orderGateway.findByIdAndOrganizationId(orderId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        tech.bystep.planificador.model.PaymentRecord record = tech.bystep.planificador.model.PaymentRecord.builder()
                .orderId(orderId).amount(amount).paymentDate(paymentDate)
                .paymentMethod(paymentMethod).notes(notes).createdAt(LocalDateTime.now())
                .build();
        tech.bystep.planificador.model.PaymentRecord saved = paymentRecordGateway.save(record);
        // Recalculate cached paymentAmount on order
        java.math.BigDecimal total = paymentRecordGateway.sumAmountByOrderId(orderId);
        order.setPaymentAmount(total);
        order.setUpdatedAt(LocalDateTime.now());
        orderGateway.save(order);
        notifyClientPayment(order, saved);
        return saved;
    }

    public List<tech.bystep.planificador.model.PaymentRecord> getPaymentRecords(UUID orderId, UUID organizationId) {
        orderGateway.findByIdAndOrganizationId(orderId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return paymentRecordGateway.findByOrderId(orderId);
    }

    public void deletePaymentRecord(UUID orderId, UUID organizationId, UUID recordId) {
        Order order = orderGateway.findByIdAndOrganizationId(orderId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        paymentRecordGateway.findById(recordId)
                // Aislamiento: el abono debe pertenecer a ESTE pedido (que ya se validó que es de la organización).
                .filter(r -> orderId.equals(r.getOrderId()))
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found: " + recordId));
        paymentRecordGateway.deleteById(recordId);
        java.math.BigDecimal total = paymentRecordGateway.sumAmountByOrderId(orderId);
        order.setPaymentAmount(total);
        order.setUpdatedAt(LocalDateTime.now());
        orderGateway.save(order);
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

    // ── WhatsApp — notificaciones al cliente final (cola por organización) ────

    private void notifyClientOrderCreated(Order order) {
        clientNotifications.notify(order.getOrganizationId(), NotificationEvent.ORDER_CREATED,
                ClientNotificationUseCase.ENTITY_ORDER, order.getId(), order.getClientPhone(), order.getNotifyWhatsapp(),
                Arrays.asList(order.getClientName(), order.getOrderNumber(), order.getProductName(),
                        NotificationText.date(order.getDeliveryDate())),
                "ORDER:" + order.getId() + ":CREATED");
    }

    private void notifyClientDateChanged(Order order, LocalDate newDate) {
        clientNotifications.notify(order.getOrganizationId(), NotificationEvent.ORDER_DATE_CHANGED,
                ClientNotificationUseCase.ENTITY_ORDER, order.getId(), order.getClientPhone(), order.getNotifyWhatsapp(),
                Arrays.asList(order.getClientName(), order.getOrderNumber(), order.getProductName(),
                        NotificationText.date(newDate)),
                "ORDER:" + order.getId() + ":DATE:" + newDate);
    }

    private void notifyStatusChange(Order order, ProgressStatus status) {
        NotificationEvent event = switch (status) {
            case IN_PREPARATION -> NotificationEvent.ORDER_IN_PROGRESS;
            case READY_TO_DELIVER -> NotificationEvent.ORDER_READY;
            case DELIVERED -> NotificationEvent.ORDER_DELIVERED;
            default -> null; // otros estados: sin notificación al cliente
        };
        if (status == ProgressStatus.READY_TO_DELIVER) {
            notifyDeliveryWorkers(order);
        }
        if (event == null) return;
        clientNotifications.notify(order.getOrganizationId(), event,
                ClientNotificationUseCase.ENTITY_ORDER, order.getId(), order.getClientPhone(), order.getNotifyWhatsapp(),
                Arrays.asList(order.getClientName(), order.getOrderNumber(), order.getProductName()),
                "ORDER:" + order.getId() + ":STATUS:" + status.name());
    }

    private void notifyClientPayment(Order order, tech.bystep.planificador.model.PaymentRecord record) {
        BigDecimal total = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal paid = order.getPaymentAmount() != null ? order.getPaymentAmount() : BigDecimal.ZERO;
        clientNotifications.notify(order.getOrganizationId(), NotificationEvent.ORDER_PAYMENT,
                ClientNotificationUseCase.ENTITY_ORDER, order.getId(), order.getClientPhone(), order.getNotifyWhatsapp(),
                Arrays.asList(order.getClientName(), NotificationText.money(record.getAmount()), order.getOrderNumber(),
                        NotificationText.money(total.subtract(paid))),
                "ORDER_PAYMENT:" + record.getId());
    }

    // ── FCM — delivery worker notification on READY_TO_DELIVER ─────────────

    private void notifyDeliveryWorkers(Order order) {
        List<String> tokens = userGateway.findByOrganizationId(order.getOrganizationId()).stream()
                .filter(u -> u.getRole() == UserRole.ORG_DELIVERY)
                .filter(u -> u.getFcmToken() != null && !u.getFcmToken().isBlank())
                .map(User::getFcmToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) return;

        String title = "Nuevo pedido asignado";
        String body = String.format("Pedido #%s - %s para %s. Direccion: %s",
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
