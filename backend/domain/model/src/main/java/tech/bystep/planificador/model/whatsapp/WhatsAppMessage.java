package tech.bystep.planificador.model.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Registro de la cola de salida (outbox) e historial. Siempre pertenece a una organización. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessage {

    private UUID id;
    private UUID organizationId;
    private String eventKey;
    private String entityType;
    private UUID entityId;
    private String toPhone;
    private String templateName;
    private String languageCode;
    @Builder.Default
    private List<String> params = new ArrayList<>();
    private WhatsAppMessageStatus status;
    private String skipReason;
    private String wamid;
    private String errorCode;
    private String errorMessage;
    private int attempts;
    private LocalDateTime nextAttemptAt;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
}
