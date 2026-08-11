package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.Repair;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepairGateway {

    Repair save(Repair repair);

    Optional<Repair> findById(UUID id);

    Optional<Repair> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Repair> findByOrganizationIdOrderByEntryDateDesc(UUID organizationId);

    void deleteById(UUID id);
}
