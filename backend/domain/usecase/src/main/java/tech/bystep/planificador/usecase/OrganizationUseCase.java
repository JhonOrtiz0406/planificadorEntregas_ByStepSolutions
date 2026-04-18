package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.gateways.OrganizationGateway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class OrganizationUseCase {

    private final OrganizationGateway organizationGateway;

    public Organization create(Organization organization) {
        String slug = generateSlug(organization.getName());
        if (organizationGateway.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        organization.setSlug(slug);
        organization.setActive(true);
        organization.setCreatedAt(LocalDateTime.now());
        organization.setUpdatedAt(LocalDateTime.now());
        return organizationGateway.save(organization);
    }

    public Organization update(UUID id, Organization updates) {
        Organization existing = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getLogoUrl() != null) existing.setLogoUrl(updates.getLogoUrl());
        if (updates.getAdminEmail() != null) existing.setAdminEmail(updates.getAdminEmail());
        existing.setUpdatedAt(LocalDateTime.now());
        return organizationGateway.save(existing);
    }

    public Optional<Organization> findById(UUID id) {
        return organizationGateway.findById(id);
    }

    public Optional<Organization> findBySlug(String slug) {
        return organizationGateway.findBySlug(slug);
    }

    public List<Organization> findAll() {
        return organizationGateway.findAll();
    }

    public void deactivate(UUID id) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setActive(false);
        org.setUpdatedAt(LocalDateTime.now());
        organizationGateway.save(org);
    }

    public void activate(UUID id) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setActive(true);
        org.setUpdatedAt(LocalDateTime.now());
        organizationGateway.save(org);
    }

    public void delete(UUID id) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        if (org.isActive()) {
            throw new IllegalStateException("Organization must be disabled before deletion");
        }
        organizationGateway.deleteById(id);
    }

    public Organization updateIcon(UUID id, String iconName) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setIconName(iconName);
        org.setUpdatedAt(LocalDateTime.now());
        return organizationGateway.save(org);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
