package tech.bystep.planificador.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.usecase.ReminderUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerConfig {

    private final ReminderUseCase reminderUseCase;

    // Todos los días a las 8:00 a.m. hora Colombia (el contenedor corre en UTC).
    // Recordatorios INTERNOS (push al equipo). Las notificaciones al cliente salen de inmediato.
    @Scheduled(cron = "0 0 8 * * *", zone = "America/Bogota")
    public void processDeliveryReminders() {
        log.info("Processing daily delivery reminders...");
        try {
            reminderUseCase.processDailyReminders();
            log.info("Daily reminders processed successfully");
        } catch (Exception e) {
            log.error("Error processing daily reminders: {}", e.getMessage(), e);
        }
    }
}
