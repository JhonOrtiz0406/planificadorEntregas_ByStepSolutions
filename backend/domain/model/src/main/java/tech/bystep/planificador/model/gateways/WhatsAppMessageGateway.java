package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WhatsAppMessageGateway {
    WhatsAppMessage save(WhatsAppMessage message);
    boolean existsByIdempotencyKey(String idempotencyKey);
    /**
     * Toma (y marca como SENDING, sumando un intento) hasta {@code limit} mensajes PENDING
     * vencidos. Seguro ante ejecuciones concurrentes (FOR UPDATE SKIP LOCKED).
     */
    List<WhatsAppMessage> claimDue(LocalDateTime now, int limit);
    /** Devuelve a PENDING los mensajes que quedaron en SENDING (p. ej. reinicio del servidor). */
    int releaseStuck(LocalDateTime olderThan);
    List<WhatsAppMessage> findRecentByOrganization(UUID organizationId, int limit);
    Map<String, Long> countByStatusSince(UUID organizationId, LocalDateTime since);
}
