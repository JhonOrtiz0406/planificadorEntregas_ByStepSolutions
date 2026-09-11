package tech.bystep.planificador.jpa.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.model.gateways.WhatsAppTemplateGateway;
import tech.bystep.planificador.model.whatsapp.WhatsAppTemplateState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WhatsAppTemplateAdapter implements WhatsAppTemplateGateway {

    private final WhatsAppTemplateJpaRepository repository;

    @Override
    public List<WhatsAppTemplateState> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationId(organizationId).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<WhatsAppTemplateState> find(UUID organizationId, String templateKey, String languageCode) {
        return repository.findByOrganizationIdAndTemplateKeyAndLanguageCode(organizationId, templateKey, languageCode)
                .map(this::toModel);
    }

    @Override
    public WhatsAppTemplateState save(WhatsAppTemplateState s) {
        WhatsAppTemplateEntity entity = WhatsAppTemplateEntity.builder()
                .id(s.getId())
                .organizationId(s.getOrganizationId())
                .templateKey(s.getTemplateKey())
                .templateName(s.getTemplateName())
                .languageCode(s.getLanguageCode() != null ? s.getLanguageCode() : "es")
                .metaTemplateId(s.getMetaTemplateId())
                .status(s.getStatus() != null ? s.getStatus() : WhatsAppTemplateState.NOT_CREATED)
                .rejectionReason(s.getRejectionReason())
                .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : LocalDateTime.now())
                .build();
        return toModel(repository.save(entity));
    }

    private WhatsAppTemplateState toModel(WhatsAppTemplateEntity e) {
        return WhatsAppTemplateState.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .templateKey(e.getTemplateKey())
                .templateName(e.getTemplateName())
                .languageCode(e.getLanguageCode())
                .metaTemplateId(e.getMetaTemplateId())
                .status(e.getStatus())
                .rejectionReason(e.getRejectionReason())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
