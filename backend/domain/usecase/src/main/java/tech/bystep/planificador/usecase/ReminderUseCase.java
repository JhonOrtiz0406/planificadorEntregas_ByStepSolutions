package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.Order;
import tech.bystep.planificador.model.ProgressStatus;
import tech.bystep.planificador.model.Reminder;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.model.gateways.NotificationGateway;
import tech.bystep.planificador.model.gateways.OrderGateway;
import tech.bystep.planificador.model.gateways.ReminderGateway;
import tech.bystep.planificador.model.gateways.UserGateway;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReminderUseCase {

    private final ReminderGateway reminderGateway;
    private final OrderGateway orderGateway;
    private final UserGateway userGateway;
    private final NotificationGateway notificationGateway;

    public void processDailyReminders() {
        LocalDate today = LocalDate.now();
        List<Reminder> pendingReminders = reminderGateway.findPendingByDate(today);

        for (Reminder reminder : pendingReminders) {
            orderGateway.findById(reminder.getOrderId()).ifPresent(order -> {
                if (order.getProgressStatus() != ProgressStatus.DELIVERED) {
                    sendDeliveryReminder(order, reminder);
                    reminderGateway.markAsSent(reminder.getId());
                }
            });
        }
    }

    private void sendDeliveryReminder(Order order, Reminder reminder) {
        String title = buildTitle(reminder.getDaysBefore());
        String body = String.format("Pedido #%s - %s para %s",
                order.getOrderNumber(), order.getProductName(), order.getClientName());

        Map<String, String> data = Map.of(
                "orderId", order.getId().toString(),
                "orderNumber", order.getOrderNumber(),
                "type", "DELIVERY_REMINDER",
                "daysBefore", String.valueOf(reminder.getDaysBefore())
        );

        List<String> tokens = getOrgUserTokens(order.getOrganizationId());
        if (!tokens.isEmpty()) {
            notificationGateway.sendToMultipleTokens(tokens, title, body, data);
        }
    }

    private List<String> getOrgUserTokens(UUID organizationId) {
        return userGateway.findByOrganizationId(organizationId).stream()
                .filter(user -> user.getFcmToken() != null && !user.getFcmToken().isBlank())
                .filter(user -> user.getRole() == UserRole.ORG_ADMIN || user.getRole() == UserRole.ORG_EMPLOYEE)
                .map(User::getFcmToken)
                .collect(Collectors.toList());
    }

    private String buildTitle(int daysBefore) {
        return switch (daysBefore) {
            case 0 -> "¡Entrega hoy!";
            case 1 -> "¡Entrega mañana!";
            default -> "Entrega en " + daysBefore + " días";
        };
    }
}
