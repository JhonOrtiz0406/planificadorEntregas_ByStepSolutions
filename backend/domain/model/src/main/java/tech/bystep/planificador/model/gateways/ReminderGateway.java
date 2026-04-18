package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.Reminder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReminderGateway {

    Reminder save(Reminder reminder);

    List<Reminder> findByOrderId(UUID orderId);

    List<Reminder> findPendingByDate(LocalDate date);

    void deleteByOrderId(UUID orderId);

    void markAsSent(UUID reminderId);
}
