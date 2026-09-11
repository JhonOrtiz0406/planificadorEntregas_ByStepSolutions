package tech.bystep.planificador.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserOrganization;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.usecase.UserUseCase;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verifica en cada request (con caché corta) que el JWT siga siendo válido para la
 * organización que trae: usuario activo, membresía habilitada en ESA organización y
 * mismo rol. Así, si sacas o inhabilitas a alguien, pierde el acceso en segundos y
 * no cuando expire su token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantAccessVerifier {

    private static final long TTL_MS = 30_000L;
    private static final int MAX_ENTRIES = 10_000;

    private final UserUseCase userUseCase;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(boolean allowed, long expiresAt) {
    }

    public boolean hasAccess(String userId, String role, String organizationId) {
        String key = userId + "|" + role + "|" + organizationId;
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt() > now) {
            return cached.allowed();
        }
        Boolean allowed = check(userId, role, organizationId);
        if (allowed == null) {
            // Error consultando la BD: no expulsamos al usuario por una falla temporal.
            return true;
        }
        if (cache.size() > MAX_ENTRIES) cache.clear();
        cache.put(key, new CacheEntry(allowed, now + TTL_MS));
        return allowed;
    }

    private Boolean check(String userId, String role, String organizationId) {
        try {
            if (userId == null || role == null) return false;
            UUID uid = UUID.fromString(userId);
            Optional<User> user = userUseCase.findById(uid);
            if (user.isEmpty() || !user.get().isActive()) return false;

            if (UserRole.PLATFORM_ADMIN.name().equals(role)) {
                return user.get().getRole() == UserRole.PLATFORM_ADMIN;
            }
            if (organizationId == null || organizationId.isBlank()) return false;
            UUID oid = UUID.fromString(organizationId);

            Optional<UserOrganization> membership = userUseCase.findMembership(uid, oid);
            if (membership.isPresent()) {
                return membership.get().isActive() && role.equals(membership.get().getRole());
            }
            // Compatibilidad: usuarios antiguos sin fila en user_organizations.
            return oid.equals(user.get().getOrganizationId())
                    && user.get().getRole() != null
                    && role.equals(user.get().getRole().name());
        } catch (IllegalArgumentException e) {
            return false; // ids mal formados en el token
        } catch (Exception e) {
            log.warn("No se pudo verificar el acceso del usuario {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
