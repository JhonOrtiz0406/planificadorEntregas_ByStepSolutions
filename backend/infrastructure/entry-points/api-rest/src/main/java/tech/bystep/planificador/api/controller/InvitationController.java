package tech.bystep.planificador.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.model.Invitation;
import tech.bystep.planificador.usecase.InvitationUseCase;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationUseCase invitationUseCase;

    @GetMapping("/accept/{token}")
    public ResponseEntity<ApiResponse<Invitation>> validateInvitation(@PathVariable("token") String token) {
        return invitationUseCase.findByToken(token)
                .filter(inv -> !inv.isExpired() && !inv.isAccepted())
                .map(inv -> ResponseEntity.ok(ApiResponse.ok("Valid invitation", inv)))
                .orElse(ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired invitation")));
    }
}
