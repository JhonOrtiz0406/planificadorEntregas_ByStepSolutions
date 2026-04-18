package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.bystep.planificador.jpa.entity.OrganizationEntity;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, UUID> {
    Optional<OrganizationEntity> findBySlug(String slug);
    Optional<OrganizationEntity> findByAdminEmail(String adminEmail);
    boolean existsBySlug(String slug);
}
