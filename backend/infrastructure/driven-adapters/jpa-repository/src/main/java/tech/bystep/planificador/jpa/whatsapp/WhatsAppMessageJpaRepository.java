package tech.bystep.planificador.jpa.whatsapp;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WhatsAppMessageJpaRepository extends JpaRepository<WhatsAppMessageEntity, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    /** Debe ejecutarse dentro de una transacción: bloquea las filas tomadas hasta el commit. */
    @Query(value = "SELECT * FROM whatsapp_messages "
            + "WHERE status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= :now) "
            + "ORDER BY created_at "
            + "LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<WhatsAppMessageEntity> lockDue(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE WhatsAppMessageEntity m SET m.status = 'PENDING', m.nextAttemptAt = :now, m.updatedAt = :now "
            + "WHERE m.status = 'SENDING' AND m.updatedAt < :olderThan")
    int releaseStuck(@Param("olderThan") LocalDateTime olderThan, @Param("now") LocalDateTime now);

    List<WhatsAppMessageEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query("SELECT m.status, COUNT(m) FROM WhatsAppMessageEntity m "
            + "WHERE m.organizationId = :orgId AND m.createdAt >= :since GROUP BY m.status")
    List<Object[]> countByStatusSince(@Param("orgId") UUID organizationId, @Param("since") LocalDateTime since);
}
