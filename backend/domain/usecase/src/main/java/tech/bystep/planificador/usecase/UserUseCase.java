package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.model.gateways.UserGateway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class UserUseCase {

    private final UserGateway userGateway;

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

    public void delete(UUID userId) {
        userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        userGateway.deleteById(userId);
    }
}
