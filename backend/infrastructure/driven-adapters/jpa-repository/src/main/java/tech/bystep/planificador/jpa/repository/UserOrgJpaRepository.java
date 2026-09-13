package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tech.bystep.planificador.jpa.entity.UserOrgEntity;

import java.util.List;
import java.util.UUID;

public interface UserOrgJpaRepository extends JpaRepository<UserOrgEntity, UserOrgEntity.UserOrgId> {
    List<UserOrgEntity> findByUserId(UUID userId);

    List<UserOrgEntity> findByOrganizationId(UUID organizationId);

    @Transactional
    @Modifying
    @Query("UPDATE UserOrgEntity uo SET uo.active = :active WHERE uo.userId = :userId AND uo.organizationId = :orgId")
    int updateActive(@Param("userId") UUID userId, @Param("orgId") UUID organizationId, @Param("active") boolean active);

    @Transactional
    @Modifying
    @Query("UPDATE UserOrgEntity uo SET uo.active = :active WHERE uo.organizationId = :orgId")
    int updateActiveForOrganization(@Param("orgId") UUID organizationId, @Param("active") boolean active);

    @Transactional
    void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    @Transactional
    void deleteByOrganizationId(UUID organizationId);
}
