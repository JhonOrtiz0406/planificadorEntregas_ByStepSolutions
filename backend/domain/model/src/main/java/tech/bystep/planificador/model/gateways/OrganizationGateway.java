package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.Organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationGateway {

    Organization save(Organization organization);

    Optional<Organization> findById(UUID id);

    Optional<Organization> findBySlug(String slug);

    Optional<Organization> findByAdminEmail(String email);

    List<Organization> findAll();

    void deleteById(UUID id);

    boolean existsBySlug(String slug);
}
