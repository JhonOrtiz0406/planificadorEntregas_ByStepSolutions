package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.Invitation;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.model.gateways.EmailGateway;
import tech.bystep.planificador.model.gateways.InvitationGateway;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class InvitationUseCase {

    private final InvitationGateway invitationGateway;
    private final EmailGateway emailGateway;

    public Invitation create(String email, UserRole role, UUID organizationId) {
        Invitation invitation = Invitation.builder()
                .email(email)
                .role(role)
                .organizationId(organizationId)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .accepted(false)
                .createdAt(LocalDateTime.now())
                .build();
        Invitation saved = invitationGateway.save(invitation);
        emailGateway.sendInvitation(email, saved.getToken());
        return saved;
    }

    public Optional<Invitation> findByToken(String token) {
        return invitationGateway.findByToken(token);
    }

    public Optional<Invitation> findPendingByEmail(String email) {
        return invitationGateway.findPendingByEmail(email);
    }

    public Invitation accept(String token) {
        Invitation invitation = invitationGateway.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        if (invitation.isExpired()) {
            throw new IllegalStateException("Invitation has expired");
        }
        if (invitation.isAccepted()) {
            throw new IllegalStateException("Invitation already accepted");
        }
        invitation.setAccepted(true);
        return invitationGateway.save(invitation);
    }
}
