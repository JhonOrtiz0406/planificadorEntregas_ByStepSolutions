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

    // Runs every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
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
