package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.bystep.planificador.jpa.entity.InvitationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationJpaRepository extends JpaRepository<InvitationEntity, UUID> {
    Optional<InvitationEntity> findByToken(String token);
    Optional<InvitationEntity> findByEmailAndOrganizationId(String email, UUID organizationId);

    @Query("SELECT i FROM InvitationEntity i WHERE LOWER(i.email) = LOWER(:email) AND i.accepted = false AND i.expiresAt > :now ORDER BY i.createdAt DESC")
    List<InvitationEntity> findPendingByEmail(@Param("email") String email, @Param("now") LocalDateTime now);
}
