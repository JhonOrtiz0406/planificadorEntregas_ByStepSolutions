package tech.bystep.planificador.jpa.adapter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.ReminderEntity;
import tech.bystep.planificador.jpa.repository.ReminderJpaRepository;
import tech.bystep.planificador.model.Reminder;
import tech.bystep.planificador.model.gateways.ReminderGateway;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReminderAdapter implements ReminderGateway {

    private final ReminderJpaRepository repository;

    @Override
    public Reminder save(Reminder reminder) {
        return toModel(repository.save(toEntity(reminder)));
    }

    @Override
    public List<Reminder> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<Reminder> findPendingByDate(LocalDate date) {
        return repository.findPendingByDate(date).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByOrderId(UUID orderId) {
        repository.deleteByOrderId(orderId);
    }

    @Override
    @Transactional
    public void markAsSent(UUID reminderId) {
        repository.findById(reminderId).ifPresent(r -> {
            r.setSent(true);
            r.setSentAt(LocalDateTime.now());
            repository.save(r);
        });
    }

    private Reminder toModel(ReminderEntity e) {
        return Reminder.builder()
                .id(e.getId()).orderId(e.getOrderId()).reminderDate(e.getReminderDate())
                .daysBefore(e.getDaysBefore()).sent(e.isSent()).sentAt(e.getSentAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ReminderEntity toEntity(Reminder m) {
        return ReminderEntity.builder()
                .id(m.getId()).orderId(m.getOrderId()).reminderDate(m.getReminderDate())
                .daysBefore(m.getDaysBefore()).sent(m.isSent()).sentAt(m.getSentAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
