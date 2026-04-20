package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.Invitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationGateway {

    Invitation save(Invitation invitation);

    Optional<Invitation> findByToken(String token);

    Optional<Invitation> findByEmailAndOrganizationId(String email, UUID organizationId);

    List<Invitation> findAllPendingByEmail(String email);

    void deleteById(UUID id);
}
