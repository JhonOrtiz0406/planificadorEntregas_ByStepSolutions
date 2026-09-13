package tech.bystep.planificador.jpa.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization_whatsapp_config")
public class WhatsAppConfigEntity {

    @Id
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private String status;

    @Column(name = "waba_id")
    private String wabaId;

    @Column(name = "phone_number_id")
    private String phoneNumberId;

    @Column(name = "display_phone_number")
    private String displayPhoneNumber;

    @Column(name = "verified_name")
    private String verifiedName;

    @ToString.Exclude
    @Column(name = "access_token_enc", columnDefinition = "TEXT")
    private String accessTokenEnc;

    @Column(name = "graph_api_version")
    private String graphApiVersion;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(name = "support_contact_text")
    private String supportContactText;

    @Column(name = "quality_rating")
    private String qualityRating;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
