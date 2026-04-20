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
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import tech.bystep.planificador.api.RateLimitService;
import tech.bystep.planificador.api.dto.request.ContactSupportRequest;
import tech.bystep.planificador.api.dto.request.SelectOrgRequest;
import tech.bystep.planificador.api.dto.response.OrgChoiceDto;
import tech.bystep.planificador.model.UserOrganization;
import tech.bystep.planificador.security.GoogleTokenVerifier;
import tech.bystep.planificador.security.JwtService;
import tech.bystep.planificador.model.gateways.EmailGateway;
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
    private final EmailGateway emailGateway;
    private final RateLimitService rateLimitService;

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
                if (!pendingInvitations.isEmpty()) {
                    return ResponseEntity.status(403).body(ApiResponse.error(
                            "Tienes una invitación pendiente. Acepta la invitación desde el enlace en tu correo electrónico."));
                }
                return ResponseEntity.status(403).body(ApiResponse.error(
                        "Acceso denegado. Necesitas una invitación para acceder a la plataforma."));
            }
        }

        if (!user.isActive()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Your account has been deactivated"));
        }

        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            userUseCase.updateFcmToken(user.getId(), request.getFcmToken());
        }

        List<UserOrganization> userOrgs = userUseCase.findUserOrganizations(user.getId());
        if (userOrgs.size() > 1) {
            List<OrgChoiceDto> choices = userOrgs.stream().map(uo -> {
                var org = organizationUseCase.findById(uo.getOrganizationId()).orElse(null);
                if (org == null) return null;
                return OrgChoiceDto.builder()
                        .id(uo.getOrganizationId().toString())
                        .name(org.getName())
                        .category(org.getCategory())
                        .iconName(org.getIconName())
                        .userRole(uo.getRole())
                        .build();
            }).filter(c -> c != null).toList();

            String selectionJwt = jwtService.generateSelectionToken(user);
            AuthResponse selectionResponse = AuthResponse.builder()
                    .requiresOrgSelection(true)
                    .selectionToken(selectionJwt)
                    .availableOrgs(choices)
                    .build();
            return ResponseEntity.ok(ApiResponse.ok("Organization selection required", selectionResponse));
        }

        if (userOrgs.isEmpty() && user.getRole() != tech.bystep.planificador.model.UserRole.PLATFORM_ADMIN) {
            AuthResponse noAccessResponse = AuthResponse.builder()
                    .noOrgAccess(true)
                    .user(UserResponse.builder()
                            .email(user.getEmail())
                            .name(user.getName())
                            .pictureUrl(user.getPictureUrl())
                            .build())
                    .build();
            return ResponseEntity.ok(ApiResponse.ok("No organization access", noAccessResponse));
        }

        if (userOrgs.size() == 1) {
            user = userUseCase.switchOrganization(user.getId(), userOrgs.get(0).getOrganizationId());
        }

        String jwt = jwtService.generateToken(user);
        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }

    @PostMapping("/select-org")
    public ResponseEntity<ApiResponse<AuthResponse>> selectOrganization(
            @Valid @RequestBody SelectOrgRequest request) {
        if (!jwtService.isTokenValid(request.getSelectionToken())
                || !jwtService.isSelectionToken(request.getSelectionToken())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid or expired selection token"));
        }

        UUID userId = jwtService.extractUserId(request.getSelectionToken());
        User user = userUseCase.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Your account has been deactivated"));
        }

        user = userUseCase.switchOrganization(userId, request.getOrganizationId());

        String jwt = jwtService.generateToken(user);
        AuthResponse authResponse = AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Organization selected", authResponse));
    }

    @PostMapping("/support-contact")
    public ResponseEntity<ApiResponse<Void>> supportContact(
            @Valid @RequestBody ContactSupportRequest request,
            HttpServletRequest httpRequest) {
        String ip = resolveClientIp(httpRequest);
        // 3 requests per IP per hour, 5 per email per day
        if (!rateLimitService.isAllowed("support:ip:" + ip, 3, 3_600_000L)
                || !rateLimitService.isAllowed("support:email:" + request.getEmail(), 5, 86_400_000L)) {
            return ResponseEntity.status(429).body(ApiResponse.error(
                    "Demasiadas solicitudes. Por favor espera antes de intentar de nuevo."));
        }
        emailGateway.sendSupportContact(
                request.getName(), request.getEmail(),
                request.getPhone(), request.getOrganizationName(),
                request.getMessage());
        return ResponseEntity.ok(ApiResponse.ok("Support request sent", null));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UserResponse toUserResponse(User user) {
        String orgName = null;
        String orgIconName = null;
        String orgCategory = null;
        if (user.getOrganizationId() != null) {
            var org = organizationUseCase.findById(user.getOrganizationId()).orElse(null);
            if (org != null) {
                orgName = org.getName();
                orgIconName = org.getIconName();
                orgCategory = org.getCategory();
            }
        }
        return UserResponse.builder()
                .id(user.getId()).email(user.getEmail()).name(user.getName())
                .pictureUrl(user.getPictureUrl()).role(user.getRole())
                .organizationId(user.getOrganizationId())
                .organizationName(orgName)
                .orgIconName(orgIconName)
                .organizationCategory(orgCategory)
                .build();
    }
}
