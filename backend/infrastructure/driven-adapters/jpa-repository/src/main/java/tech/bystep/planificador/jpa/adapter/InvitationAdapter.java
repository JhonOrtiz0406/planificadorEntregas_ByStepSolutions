package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.InvitationEntity;
import tech.bystep.planificador.jpa.repository.InvitationJpaRepository;
import tech.bystep.planificador.model.Invitation;
import tech.bystep.planificador.model.gateways.InvitationGateway;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InvitationAdapter implements InvitationGateway {

    private final InvitationJpaRepository repository;

    @Override
    public Invitation save(Invitation invitation) {
        return toModel(repository.save(toEntity(invitation)));
    }

    @Override
    public Optional<Invitation> findByToken(String token) {
        return repository.findByToken(token).map(this::toModel);
    }

    @Override
    public Optional<Invitation> findByEmailAndOrganizationId(String email, UUID organizationId) {
        return repository.findByEmailAndOrganizationId(email, organizationId).map(this::toModel);
    }

    @Override
    public List<Invitation> findAllPendingByEmail(String email) {
        return repository.findPendingByEmail(email, LocalDateTime.now())
                .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Invitation toModel(InvitationEntity e) {
        return Invitation.builder()
                .id(e.getId()).email(e.getEmail()).role(e.getRole())
                .organizationId(e.getOrganizationId()).token(e.getToken())
                .expiresAt(e.getExpiresAt()).accepted(e.isAccepted()).createdAt(e.getCreatedAt())
                .build();
    }

    private InvitationEntity toEntity(Invitation m) {
        return InvitationEntity.builder()
                .id(m.getId()).email(m.getEmail()).role(m.getRole())
                .organizationId(m.getOrganizationId()).token(m.getToken())
                .expiresAt(m.getExpiresAt()).accepted(m.isAccepted()).createdAt(m.getCreatedAt())
                .build();
    }
}
