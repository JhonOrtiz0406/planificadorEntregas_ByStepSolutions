package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppConfigGateway {
    Optional<WhatsAppConfig> findByOrganizationId(UUID organizationId);
    /** true si otro organization_id ya usa ese phone_number_id (un número = una organización). */
    boolean isPhoneNumberIdUsedByAnotherOrganization(String phoneNumberId, UUID organizationId);
    WhatsAppConfig save(WhatsAppConfig config);
}
