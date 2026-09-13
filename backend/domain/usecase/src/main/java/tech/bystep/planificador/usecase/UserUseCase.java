package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserOrganization;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.model.gateways.UserGateway;
import tech.bystep.planificador.model.gateways.UserOrgGateway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class UserUseCase {

    private final UserGateway userGateway;
    private final UserOrgGateway userOrgGateway;

    public User findOrCreateFromGoogle(String googleId, String email, String name, String pictureUrl) {
        return userGateway.findByGoogleId(googleId)
                .orElseGet(() -> userGateway.findByEmail(email)
                        .map(existing -> {
                            existing.setGoogleId(googleId);
                            existing.setPictureUrl(pictureUrl);
                            existing.setUpdatedAt(LocalDateTime.now());
                            return userGateway.save(existing);
                        })
                        .orElse(null));
    }

    public User registerFromInvitation(String googleId, String email, String name, String pictureUrl,
                                       UserRole role, UUID organizationId) {
        User saved = userGateway.findByGoogleId(googleId)
                .map(existing -> {
                    existing.setOrganizationId(organizationId);
                    existing.setRole(role);
                    existing.setPictureUrl(pictureUrl);
                    existing.setActive(true);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return userGateway.save(existing);
                })
                .orElseGet(() -> {
                    User user = User.builder()
                            .googleId(googleId)
                            .email(email)
                            .name(name)
                            .pictureUrl(pictureUrl)
                            .role(role)
                            .organizationId(organizationId)
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userGateway.save(user);
                });
        userOrgGateway.save(saved.getId(), organizationId, role.name());
        return saved;
    }

    public List<UserOrganization> findUserOrganizations(UUID userId) {
        return userOrgGateway.findByUserId(userId);
    }

    /** Membresías habilitadas: son las únicas organizaciones a las que el usuario puede entrar. */
    public List<UserOrganization> findActiveUserOrganizations(UUID userId) {
        return userOrgGateway.findByUserId(userId).stream().filter(UserOrganization::isActive).toList();
    }

    public Optional<UserOrganization> findMembership(UUID userId, UUID organizationId) {
        if (userId == null || organizationId == null) return Optional.empty();
        return userOrgGateway.find(userId, organizationId);
    }

    /** true si el usuario pertenece (habilitado o no) a la organización. */
    public boolean isMemberOf(UUID userId, UUID organizationId) {
        if (findMembership(userId, organizationId).isPresent()) return true;
        // Compatibilidad con usuarios antiguos sin fila en user_organizations.
        return userGateway.findById(userId)
                .map(u -> organizationId.equals(u.getOrganizationId()))
                .orElse(false);
    }

    /**
     * Miembros de UNA organización, con el rol y estado que tienen EN ESA organización.
     * Incluye usuarios cuya organización actual es otra pero que también pertenecen a esta.
     */
    public List<User> findMembers(UUID organizationId) {
        java.util.Map<UUID, User> result = new java.util.LinkedHashMap<>();
        for (UserOrganization membership : userOrgGateway.findByOrganizationId(organizationId)) {
            userGateway.findById(membership.getUserId()).ifPresent(u -> {
                u.setRole(UserRole.valueOf(membership.getRole()));
                u.setActive(u.isActive() && membership.isActive());
                u.setOrganizationId(organizationId);
                result.put(u.getId(), u);
            });
        }
        // Compatibilidad: usuarios con organization_id = esta org pero sin fila de membresía.
        for (User u : userGateway.findByOrganizationId(organizationId)) {
            result.putIfAbsent(u.getId(), u);
        }
        return new java.util.ArrayList<>(result.values());
    }

    /**
     * Inhabilita al usuario SOLO en esta organización. Si ya no le queda ninguna
     * organización habilitada, se inhabilita la cuenta (comportamiento de siempre
     * para usuarios de una sola organización).
     */
    public void deactivateMembership(UUID userId, UUID organizationId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userOrgGateway.setActive(userId, organizationId, false);
        List<UserOrganization> stillActive = findActiveUserOrganizations(userId);
        if (stillActive.isEmpty()) {
            user.setActive(false);
        } else if (organizationId.equals(user.getOrganizationId())) {
            UserOrganization next = stillActive.get(0);
            user.setOrganizationId(next.getOrganizationId());
            user.setRole(UserRole.valueOf(next.getRole()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userGateway.save(user);
    }

    /** Vuelve a habilitar al usuario en esta organización. */
    public void activateMembership(UUID userId, UUID organizationId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userOrgGateway.setActive(userId, organizationId, true);
        user.setActive(true);
        if (user.getOrganizationId() == null) {
            user.setOrganizationId(organizationId);
            userOrgGateway.find(userId, organizationId)
                    .ifPresent(m -> user.setRole(UserRole.valueOf(m.getRole())));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userGateway.save(user);
    }

    /**
     * Saca al usuario de esta organización. Si no pertenece a ninguna otra, se elimina
     * la cuenta (comportamiento de siempre); si pertenece a otras, se conserva para ellas.
     */
    public void removeFromOrganization(UUID userId, UUID organizationId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userOrgGateway.deleteByUserIdAndOrganizationId(userId, organizationId);
        List<UserOrganization> remaining = userOrgGateway.findByUserId(userId);
        if (remaining.isEmpty()) {
            userGateway.deleteById(userId);
            return;
        }
        if (organizationId.equals(user.getOrganizationId())) {
            UserOrganization next = remaining.stream().filter(UserOrganization::isActive).findFirst()
                    .orElse(remaining.get(0));
            user.setOrganizationId(next.getOrganizationId());
            user.setRole(UserRole.valueOf(next.getRole()));
            user.setUpdatedAt(LocalDateTime.now());
            userGateway.save(user);
        }
    }

    public User switchOrganization(UUID userId, UUID organizationId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<UserOrganization> orgs = userOrgGateway.findByUserId(userId);
        UserOrganization target = orgs.stream()
                .filter(o -> o.getOrganizationId().equals(organizationId))
                .filter(UserOrganization::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User does not belong to organization: " + organizationId));
        user.setOrganizationId(organizationId);
        user.setRole(UserRole.valueOf(target.getRole()));
        user.setUpdatedAt(LocalDateTime.now());
        return userGateway.save(user);
    }

    public User createPlatformAdmin(String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name)
                .role(UserRole.PLATFORM_ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userGateway.save(user);
    }

    public Optional<User> findById(UUID id) {
        return userGateway.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userGateway.findByEmail(email);
    }

    public List<User> findByOrganization(UUID organizationId) {
        return userGateway.findByOrganizationId(organizationId);
    }

    public List<User> findByOrganizationAndRole(UUID organizationId, UserRole role) {
        return userGateway.findByOrganizationIdAndRole(organizationId, role);
    }

    public User updateFcmToken(UUID userId, String fcmToken) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setFcmToken(fcmToken);
        user.setUpdatedAt(LocalDateTime.now());
        return userGateway.save(user);
    }

    public void deactivate(UUID userId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userGateway.save(user);
    }

    public void activate(UUID userId) {
        User user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userGateway.save(user);
    }

    public void delete(UUID userId) {
        userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userGateway.deleteById(userId);
    }
}
