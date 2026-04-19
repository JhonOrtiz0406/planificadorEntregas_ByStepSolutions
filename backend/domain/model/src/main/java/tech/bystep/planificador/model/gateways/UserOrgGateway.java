package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.UserOrganization;

import java.util.List;
import java.util.UUID;

public interface UserOrgGateway {
    void save(UUID userId, UUID organizationId, String role);
    List<UserOrganization> findByUserId(UUID userId);
    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    void deleteByOrganizationId(UUID organizationId);
}
