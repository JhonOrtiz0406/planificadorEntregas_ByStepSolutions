package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGateway {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    List<User> findByOrganizationId(UUID organizationId);

    List<User> findByOrganizationIdAndRole(UUID organizationId, UserRole role);

    void deleteById(UUID id);

    boolean existsByEmail(String email);
}
