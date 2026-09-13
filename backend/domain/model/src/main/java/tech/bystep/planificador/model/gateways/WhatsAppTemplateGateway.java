package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.whatsapp.WhatsAppTemplateState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppTemplateGateway {
    List<WhatsAppTemplateState> findByOrganizationId(UUID organizationId);
    Optional<WhatsAppTemplateState> find(UUID organizationId, String templateKey, String languageCode);
    WhatsAppTemplateState save(WhatsAppTemplateState state);
}
