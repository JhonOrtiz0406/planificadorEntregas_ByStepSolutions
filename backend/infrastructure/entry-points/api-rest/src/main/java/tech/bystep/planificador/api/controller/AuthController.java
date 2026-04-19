package tech.bystep.planificador.api.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.request.GoogleAuthRequest;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.api.dto.response.AuthResponse;
import tech.bystep.planificador.api.dto.response.UserResponse;
import tech.bystep.planificador.model.Invitation;
import tech.bystep.planificador.model.User;

import java.util.List;
import tech.bystep.planificador.security.GoogleTokenVerifier;
import tech.bystep.planificador.security.JwtService;
import tech.bystep.planificador.usecase.InvitationUseCase;
import tech.bystep.planificador.usecase.OrganizationUseCase;
import tech.bystep.planificador.usecase.UserUseCase;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserUseCase userUseCase;
    private final InvitationUseCase invitationUseCase;
    private final OrganizationUseCase organizationUseCase;

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid Google token"));
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        User user;

        if (request.getInvitationToken() != null && !request.getInvitationToken().isBlank()) {
            Invitation invitation = invitationUseCase.findByToken(request.getInvitationToken())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid invitation"));

            if (!invitation.getEmail().equalsIgnoreCase(email)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Email does not match invitation"));
            }

            invitationUseCase.accept(request.getInvitationToken());
            user = userUseCase.registerFromInvitation(googleId, email, name, pictureUrl,
                    invitation.getRole(), invitation.getOrganizationId());
        } else {
            user = userUseCase.findOrCreateFromGoogle(googleId, email, name, pictureUrl);
            if (user == null) {
                List<Invitation> pendingInvitations = invitationUseCase.findAllPendingByEmail(email);
                if (pendingInvitations.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error(
                            "Access denied. You need an invitation to join the platform."));
                }
                if (pendingInvitations.size() > 1) {
                    return ResponseEntity.status(409).body(ApiResponse.error(
                            "Multiple pending invitations found. Use the specific invitation link from your email."));
                }
                Invitation pending = pendingInvitations.get(0);
                invitationUseCase.accept(pending.getToken());
                user = userUseCase.registerFromInvitation(googleId, email, name, pictureUrl,
                        pending.getRole(), pending.getOrganizationId());
            }
        }

        if (!user.isActive()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Your account has been deactivated"));
        }

        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            userUseCase.updateFcmToken(user.getId(), request.getFcmToken());
        }

        String jwt = jwtService.generateToken(user);
        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    private UserResponse toUserResponse(User user) {
        String orgName = null;
        String orgIconName = null;
        if (user.getOrganizationId() != null) {
            var org = organizationUseCase.findById(user.getOrganizationId()).orElse(null);
            if (org != null) {
                orgName = org.getName();
                orgIconName = org.getIconName();
            }
        }
        return UserResponse.builder()
                .id(user.getId()).email(user.getEmail()).name(user.getName())
                .pictureUrl(user.getPictureUrl()).role(user.getRole())
                .organizationId(user.getOrganizationId())
                .organizationName(orgName)
                .orgIconName(orgIconName)
                .build();
    }
}
