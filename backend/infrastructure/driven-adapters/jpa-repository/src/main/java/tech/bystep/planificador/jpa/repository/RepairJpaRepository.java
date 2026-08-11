package tech.bystep.planificador.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.bystep.planificador.jpa.entity.RepairEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairJpaRepository extends JpaRepository<RepairEntity, UUID> {

    Optional<RepairEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<RepairEntity> findByOrganizationIdOrderByEntryDateDesc(UUID organizationId);
}
