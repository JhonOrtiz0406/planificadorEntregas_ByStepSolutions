package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import tech.bystep.planificador.jpa.entity.UserOrgEntity;

import java.util.List;
import java.util.UUID;

public interface UserOrgJpaRepository extends JpaRepository<UserOrgEntity, UserOrgEntity.UserOrgId> {
    List<UserOrgEntity> findByUserId(UUID userId);

    @Transactional
    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    @Transactional
    void deleteByOrganizationId(UUID organizationId);
}
