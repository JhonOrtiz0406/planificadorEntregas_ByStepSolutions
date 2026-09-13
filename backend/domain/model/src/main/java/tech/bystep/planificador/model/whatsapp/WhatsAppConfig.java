package tech.bystep.planificador.model.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Configuración de WhatsApp de UNA organización. Cada organización tiene su
 * propio WABA, número y token: nunca se comparte entre organizaciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppConfig {

    private UUID organizationId;
    /** Módulo contratado/activado por el PLATFORM_ADMIN. */
    private boolean enabled;
    private WhatsAppConnectionStatus status;
    private String wabaId;
    private String phoneNumberId;
    private String displayPhoneNumber;
    private String verifiedName;

    /** Token en claro SOLO en memoria. Se cifra al persistir y nunca se expone por la API. */
    @ToString.Exclude
    private String accessToken;

    private String graphApiVersion;
    private String languageCode;
    private String supportContactText;
    private String qualityRating;
    private String lastError;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean hasCredentials() {
        return phoneNumberId != null && !phoneNumberId.isBlank()
                && accessToken != null && !accessToken.isBlank();
    }

    public boolean isReadyToSend() {
        return enabled && status == WhatsAppConnectionStatus.CONNECTED && hasCredentials();
    }

    public String language() {
        return languageCode == null || languageCode.isBlank() ? "es" : languageCode;
    }
}
