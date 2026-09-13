package tech.bystep.planificador.jpa.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WhatsAppConfigJpaRepository extends JpaRepository<WhatsAppConfigEntity, UUID> {
    boolean existsByPhoneNumberIdAndOrganizationIdNot(String phoneNumberId, UUID organizationId);
}
