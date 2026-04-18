package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.bystep.planificador.jpa.entity.UserEntity;
import tech.bystep.planificador.model.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByGoogleId(String googleId);
    List<UserEntity> findByOrganizationId(UUID organizationId);
    List<UserEntity> findByOrganizationIdAndRole(UUID organizationId, UserRole role);
    boolean existsByEmail(String email);
}
