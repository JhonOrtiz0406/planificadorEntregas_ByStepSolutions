package tech.bystep.planificador.model.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Estado de una plantilla del catálogo en el WABA de una organización. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppTemplateState {

    public static final String APPROVED = "APPROVED";
    public static final String NOT_CREATED = "NOT_CREATED";

    private UUID id;
    private UUID organizationId;
    private String templateKey;
    private String templateName;
    private String languageCode;
    private String metaTemplateId;
    /** NOT_CREATED | PENDING | APPROVED | REJECTED | PAUSED | DISABLED | ERROR (texto de Meta). */
    private String status;
    private String rejectionReason;
    private LocalDateTime updatedAt;

    public boolean isApproved() {
        return APPROVED.equalsIgnoreCase(status);
    }
}
