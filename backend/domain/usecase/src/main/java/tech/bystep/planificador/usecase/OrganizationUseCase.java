package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserOrganization;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.model.gateways.EmailGateway;
import tech.bystep.planificador.model.gateways.OrganizationGateway;
import tech.bystep.planificador.model.gateways.UserGateway;
import tech.bystep.planificador.model.gateways.UserOrgGateway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class OrganizationUseCase {

    private final OrganizationGateway organizationGateway;
    private final UserGateway userGateway;
    private final UserOrgGateway userOrgGateway;
    private final EmailGateway emailGateway;

    public Organization create(Organization organization) {
        String slug = generateSlug(organization.getName());
        if (organizationGateway.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        organization.setSlug(slug);
        organization.setAdminFirstName(blankToNull(organization.getAdminFirstName()));
        organization.setAdminLastName(blankToNull(organization.getAdminLastName()));
        if (organization.getAdminPhone() != null) {
            organization.setAdminPhone(normalizePhoneField(organization.getAdminPhone(), "celular personal"));
        }
        if (organization.getOrganizationPhone() != null) {
            organization.setOrganizationPhone(normalizePhoneField(organization.getOrganizationPhone(), "celular de la organización"));
        }
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
        if (updates.getAdminFirstName() != null) existing.setAdminFirstName(blankToNull(updates.getAdminFirstName()));
        if (updates.getAdminLastName() != null) existing.setAdminLastName(blankToNull(updates.getAdminLastName()));
        if (updates.getAdminPhone() != null) existing.setAdminPhone(normalizePhoneField(updates.getAdminPhone(), "celular personal"));
        if (updates.getOrganizationPhone() != null) existing.setOrganizationPhone(normalizePhoneField(updates.getOrganizationPhone(), "celular de la organización"));
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
        revokeOrgAccess(id, org.getName(), false);
    }

    public void activate(UUID id) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setActive(true);
        org.setUpdatedAt(LocalDateTime.now());
        organizationGateway.save(org);
        // Re-activa las membresías de ESTA organización (y la cuenta de esos usuarios).
        userOrgGateway.setActiveForOrganization(id, true);
        for (User member : membersOf(id)) {
            member.setActive(true);
            member.setUpdatedAt(LocalDateTime.now());
            userGateway.save(member);
        }
        if (org.getAdminEmail() != null && !org.getAdminEmail().isBlank()) {
            try { emailGateway.sendOrgReactivated(org.getAdminEmail(), org.getName()); }
            catch (Exception ignored) {}
        }
    }

    public void delete(UUID id) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        if (org.isActive()) {
            throw new IllegalStateException("Organization must be disabled before deletion");
        }
        revokeOrgAccess(id, org.getName(), true);
        organizationGateway.deleteById(id);
    }

    private void revokeOrgAccess(UUID orgId, String orgName, boolean deleted) {
        java.util.Map<UUID, String> roleInOrg = rolesIn(orgId);
        List<User> members = membersOf(orgId);
        userOrgGateway.setActiveForOrganization(orgId, false);

        String adminEmail = members.stream()
                .filter(u -> UserRole.ORG_ADMIN.name().equals(roleInOrg.getOrDefault(u.getId(), u.getRole().name())))
                .map(User::getEmail)
                .findFirst()
                .orElse(null);

        for (User member : members) {
            boolean isAdmin = UserRole.ORG_ADMIN.name()
                    .equals(roleInOrg.getOrDefault(member.getId(), member.getRole().name()));
            try {
                if (isAdmin) {
                    emailGateway.sendOrgClosedToAdmin(member.getEmail(), orgName, deleted);
                } else {
                    emailGateway.sendOrgClosedToMember(member.getEmail(), orgName, adminEmail, deleted);
                }
            } catch (Exception ignored) {}

            // Aislamiento: si el usuario también pertenece a OTRA organización habilitada,
            // conserva su acceso allá; solo pierde el acceso a esta.
            List<UserOrganization> otherActive = userOrgGateway.findByUserId(member.getId()).stream()
                    .filter(UserOrganization::isActive)
                    .filter(m -> !orgId.equals(m.getOrganizationId()))
                    .toList();
            if (otherActive.isEmpty()) {
                member.setActive(false);
                if (deleted) {
                    member.setOrganizationId(null);
                }
            } else if (orgId.equals(member.getOrganizationId())) {
                member.setOrganizationId(otherActive.get(0).getOrganizationId());
                member.setRole(UserRole.valueOf(otherActive.get(0).getRole()));
            }
            member.setUpdatedAt(LocalDateTime.now());
            userGateway.save(member);
        }

        if (deleted) {
            userOrgGateway.deleteByOrganizationId(orgId);
        }
    }

    public Organization updateIcon(UUID id, String iconName) {
        Organization org = organizationGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        org.setIconName(iconName);
        org.setUpdatedAt(LocalDateTime.now());
        return organizationGateway.save(org);
    }

    /** Usuarios de la organización: por membresía y, por compatibilidad, por organization_id. */
    private List<User> membersOf(UUID orgId) {
        java.util.Map<UUID, User> result = new java.util.LinkedHashMap<>();
        for (UserOrganization membership : userOrgGateway.findByOrganizationId(orgId)) {
            userGateway.findById(membership.getUserId()).ifPresent(u -> result.put(u.getId(), u));
        }
        for (User u : userGateway.findByOrganizationId(orgId)) {
            result.putIfAbsent(u.getId(), u);
        }
        return new java.util.ArrayList<>(result.values());
    }

    private java.util.Map<UUID, String> rolesIn(UUID orgId) {
        java.util.Map<UUID, String> roles = new java.util.HashMap<>();
        for (UserOrganization membership : userOrgGateway.findByOrganizationId(orgId)) {
            roles.put(membership.getUserId(), membership.getRole());
        }
        return roles;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Guarda celulares en formato internacional sin '+', ej. 573001234567. Vacío = null. */
    private static String normalizePhoneField(String value, String fieldLabel) {
        if (value == null || value.isBlank()) return null;
        String normalized = NotificationText.normalizePhone(value);
        if (normalized == null) {
            throw new IllegalArgumentException("El " + fieldLabel + " no es un celular válido");
        }
        return normalized;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
