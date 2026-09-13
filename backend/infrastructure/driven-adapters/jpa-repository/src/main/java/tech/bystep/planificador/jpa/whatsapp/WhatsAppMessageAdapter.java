package tech.bystep.planificador.jpa.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.bystep.planificador.model.gateways.WhatsAppMessageGateway;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessageStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WhatsAppMessageAdapter implements WhatsAppMessageGateway {

    private final WhatsAppMessageJpaRepository repository;

    @Override
    public WhatsAppMessage save(WhatsAppMessage message) {
        return toModel(repository.save(toEntity(message)));
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return repository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional
    public List<WhatsAppMessage> claimDue(LocalDateTime now, int limit) {
        List<WhatsAppMessageEntity> locked = repository.lockDue(now, limit);
        List<WhatsAppMessage> claimed = new ArrayList<>();
        for (WhatsAppMessageEntity e : locked) {
            e.setStatus(WhatsAppMessageStatus.SENDING.name());
            e.setAttempts(e.getAttempts() + 1);
            e.setUpdatedAt(now);
            claimed.add(toModel(repository.save(e)));
        }
        return claimed;
    }

    @Override
    @Transactional
    public int releaseStuck(LocalDateTime olderThan) {
        return repository.releaseStuck(olderThan, LocalDateTime.now());
    }

    @Override
    public List<WhatsAppMessage> findRecentByOrganization(UUID organizationId, int limit) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, PageRequest.of(0, limit))
                .stream().map(this::toModel).toList();
    }

    @Override
    public Map<String, Long> countByStatusSince(UUID organizationId, LocalDateTime since) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : repository.countByStatusSince(organizationId, since)) {
            counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private WhatsAppMessageEntity toEntity(WhatsAppMessage m) {
        LocalDateTime now = LocalDateTime.now();
        return WhatsAppMessageEntity.builder()
                .id(m.getId())
                .organizationId(m.getOrganizationId())
                .eventKey(m.getEventKey())
                .entityType(m.getEntityType())
                .entityId(m.getEntityId())
                .toPhone(m.getToPhone())
                .templateName(m.getTemplateName())
                .languageCode(m.getLanguageCode())
                .params(m.getParams() != null ? new ArrayList<>(m.getParams()) : new ArrayList<>())
                .status((m.getStatus() != null ? m.getStatus() : WhatsAppMessageStatus.PENDING).name())
                .skipReason(m.getSkipReason())
                .wamid(m.getWamid())
                .errorCode(truncate(m.getErrorCode(), 20))
                .errorMessage(m.getErrorMessage())
                .attempts(m.getAttempts())
                .nextAttemptAt(m.getNextAttemptAt())
                .idempotencyKey(m.getIdempotencyKey())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt() : now)
                .updatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt() : now)
                .sentAt(m.getSentAt())
                .build();
    }

    private WhatsAppMessage toModel(WhatsAppMessageEntity e) {
        WhatsAppMessageStatus status;
        try {
            status = WhatsAppMessageStatus.valueOf(e.getStatus());
        } catch (Exception ex) {
            status = WhatsAppMessageStatus.FAILED;
        }
        return WhatsAppMessage.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .eventKey(e.getEventKey())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .toPhone(e.getToPhone())
                .templateName(e.getTemplateName())
                .languageCode(e.getLanguageCode())
                .params(e.getParams() != null ? new ArrayList<>(e.getParams()) : new ArrayList<>())
                .status(status)
                .skipReason(e.getSkipReason())
                .wamid(e.getWamid())
                .errorCode(e.getErrorCode())
                .errorMessage(e.getErrorMessage())
                .attempts(e.getAttempts())
                .nextAttemptAt(e.getNextAttemptAt())
                .idempotencyKey(e.getIdempotencyKey())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .sentAt(e.getSentAt())
                .build();
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
