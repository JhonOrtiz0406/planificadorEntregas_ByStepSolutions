package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.UserOrganization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserOrgGateway {
    /** Crea o reemplaza la membresía (queda activa). */
    void save(UUID userId, UUID organizationId, String role);
    List<UserOrganization> findByUserId(UUID userId);
    List<UserOrganization> findByOrganizationId(UUID organizationId);
    Optional<UserOrganization> find(UUID userId, UUID organizationId);
    void setActive(UUID userId, UUID organizationId, boolean active);
    void setActiveForOrganization(UUID organizationId, boolean active);
    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    void deleteByOrganizationId(UUID organizationId);
}
