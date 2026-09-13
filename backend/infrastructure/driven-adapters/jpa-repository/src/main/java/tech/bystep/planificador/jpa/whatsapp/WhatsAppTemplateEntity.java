package tech.bystep.planificador.jpa.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization_whatsapp_templates")
public class WhatsAppTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "template_key", nullable = false)
    private String templateKey;

    @Column(name = "template_name", nullable = false)
    private String templateName;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(name = "meta_template_id")
    private String metaTemplateId;

    @Column(nullable = false)
    private String status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
