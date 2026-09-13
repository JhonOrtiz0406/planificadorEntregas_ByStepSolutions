package tech.bystep.planificador.jpa.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppTemplateJpaRepository extends JpaRepository<WhatsAppTemplateEntity, UUID> {
    List<WhatsAppTemplateEntity> findByOrganizationId(UUID organizationId);

    Optional<WhatsAppTemplateEntity> findByOrganizationIdAndTemplateKeyAndLanguageCode(
            UUID organizationId, String templateKey, String languageCode);
}
