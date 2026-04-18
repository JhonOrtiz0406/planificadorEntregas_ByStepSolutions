package tech.bystep.planificador.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reminder {

    private UUID id;
    private UUID orderId;
    private LocalDate reminderDate;
    private int daysBefore;
    private boolean sent;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
