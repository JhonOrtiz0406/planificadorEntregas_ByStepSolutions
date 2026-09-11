package tech.bystep.planificador.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.request.CreateOrganizationRequest;
import tech.bystep.planificador.api.dto.request.InviteMemberRequest;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.model.Invitation;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.security.UserPrincipal;
import tech.bystep.planificador.model.gateways.EmailGateway;
import tech.bystep.planificador.usecase.InvitationUseCase;
import tech.bystep.planificador.usecase.OrganizationUseCase;
import tech.bystep.planificador.usecase.UserUseCase;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationUseCase organizationUseCase;
    private final UserUseCase userUseCase;
    private final InvitationUseCase invitationUseCase;
    private final EmailGateway emailGateway;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Organization>>> getAllOrganizations() {
        return ResponseEntity.ok(ApiResponse.ok(organizationUseCase.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Organization>> getOrganization(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccessOrg(principal, id)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        return organizationUseCase.findById(id)
                .map(o -> ResponseEntity.ok(ApiResponse.ok(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Organization>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        Organization org = Organization.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .adminEmail(request.getAdminEmail())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .adminFirstName(request.getAdminFirstName())
                .adminLastName(request.getAdminLastName())
                .adminPhone(request.getAdminPhone())
                .organizationPhone(request.getOrganizationPhone())
                .build();
        Organization created = organizationUseCase.create(org);
        invitationUseCase.create(request.getAdminEmail(), tech.bystep.planificador.model.UserRole.ORG_ADMIN, created.getId(), created.getName());
        return ResponseEntity.status(201).body(ApiResponse.ok("Organization created and invitation sent", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Organization>> updateOrganization(
            @PathVariable("id") UUID id,
            @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("ORG_ADMIN".equals(principal.getRole()) && !id.toString().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        Organization updates = Organization.builder()
                .name(request.getName()).logoUrl(request.getLogoUrl()).adminEmail(request.getAdminEmail())
                .adminFirstName(request.getAdminFirstName()).adminLastName(request.getAdminLastName())
                .adminPhone(request.getAdminPhone()).organizationPhone(request.getOrganizationPhone())
                .build();
        Organization updated = organizationUseCase.update(id, updates);
        return ResponseEntity.ok(ApiResponse.ok("Organization updated", updated));
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Invitation>> inviteMember(
            @PathVariable("id") UUID id,
            @Valid @RequestBody InviteMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("ORG_ADMIN".equals(principal.getRole())) {
            if (!id.toString().equals(principal.getOrganizationId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            // ORG_ADMIN can only invite employees and delivery workers, not admins
            if (request.getRole() == tech.bystep.planificador.model.UserRole.PLATFORM_ADMIN
                    || request.getRole() == tech.bystep.planificador.model.UserRole.ORG_ADMIN) {
                return ResponseEntity.status(403).body(ApiResponse.error(
                        "Organization admin can only invite employees or delivery workers"));
            }
        }
        Organization org = organizationUseCase.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
        Invitation invitation = invitationUseCase.create(request.getEmail(), request.getRole(), id, org.getName());
        return ResponseEntity.ok(ApiResponse.ok("Invitation sent to " + request.getEmail(), invitation));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("ORG_ADMIN".equals(principal.getRole()) && !id.toString().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        // Sin googleId ni token FCM: solo lo que la pantalla necesita.
        List<MemberResponse> members = userUseCase.findMembers(id).stream()
                .map(u -> new MemberResponse(u.getId(), u.getEmail(), u.getName(), u.getPictureUrl(),
                        u.getRole() != null ? u.getRole().name() : null, id, u.isActive(), u.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // ORG_ADMIN scope check
        if ("ORG_ADMIN".equals(principal.getRole()) && !id.toString().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        // Resolve target user and enforce role restrictions
        User target = memberOf(userId, id);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }
        // ORG_ADMIN cannot deactivate another ORG_ADMIN or PLATFORM_ADMIN
        if ("ORG_ADMIN".equals(principal.getRole())) {
            String targetRole = target.getRole().name();
            if ("ORG_ADMIN".equals(targetRole) || "PLATFORM_ADMIN".equals(targetRole)) {
                return ResponseEntity.status(403).body(ApiResponse.error(
                        "Organization admin cannot deactivate an admin account"));
            }
        }
        userUseCase.deactivateMembership(userId, id);
        String orgName = organizationUseCase.findById(id).map(o -> o.getName()).orElse("tu organización");
        emailGateway.sendMemberDeactivated(target.getEmail(), target.getName(), orgName);
        return ResponseEntity.ok(ApiResponse.ok("Member deactivated", null));
    }

    @PatchMapping("/{id}/members/{userId}/enable")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> enableMember(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("ORG_ADMIN".equals(principal.getRole()) && !id.toString().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        User target = memberOf(userId, id);
        if (target == null) return ResponseEntity.notFound().build();
        if ("ORG_ADMIN".equals(principal.getRole())) {
            String targetRole = target.getRole().name();
            if ("ORG_ADMIN".equals(targetRole) || "PLATFORM_ADMIN".equals(targetRole)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Organization admin cannot enable an admin account"));
            }
        }
        userUseCase.activateMembership(userId, id);
        String orgName = organizationUseCase.findById(id).map(o -> o.getName()).orElse("tu organización");
        emailGateway.sendMemberReactivated(target.getEmail(), target.getName(), orgName);
        return ResponseEntity.ok(ApiResponse.ok("Member enabled", null));
    }

    @DeleteMapping("/{id}/members/{userId}/permanent")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMemberPermanently(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("ORG_ADMIN".equals(principal.getRole()) && !id.toString().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        User target = memberOf(userId, id);
        if (target == null) return ResponseEntity.notFound().build();
        if ("ORG_ADMIN".equals(principal.getRole())) {
            String targetRole = target.getRole().name();
            if ("ORG_ADMIN".equals(targetRole) || "PLATFORM_ADMIN".equals(targetRole)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Cannot delete admin accounts"));
            }
        }
        String orgName = organizationUseCase.findById(id).map(o -> o.getName()).orElse("tu organización");
        String email = target.getEmail();
        String name  = target.getName();
        userUseCase.removeFromOrganization(userId, id);
        emailGateway.sendMemberDeleted(email, name, orgName);
        return ResponseEntity.ok(ApiResponse.ok("Member permanently deleted", null));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> disableOrganization(@PathVariable("id") UUID id) {
        organizationUseCase.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Organization disabled", null));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> enableOrganization(@PathVariable("id") UUID id) {
        organizationUseCase.activate(id);
        return ResponseEntity.ok(ApiResponse.ok("Organization enabled", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable("id") UUID id) {
        organizationUseCase.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Organization deleted", null));
    }

    @PatchMapping("/{id}/icon")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Organization>> updateIcon(
            @PathVariable("id") UUID id,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        if ("ORG_ADMIN".equals(principal.getRole()) && !id.toString().equals(principal.getOrganizationId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        String iconName = body.get("iconName");
        Organization updated = organizationUseCase.updateIcon(id, iconName);
        return ResponseEntity.ok(ApiResponse.ok("Icon updated", updated));
    }

    /** PLATFORM_ADMIN ve todas; cualquier otro rol solo su propia organización. */
    private boolean canAccessOrg(UserPrincipal principal, UUID orgId) {
        if (principal == null) return false;
        if ("PLATFORM_ADMIN".equals(principal.getRole())) return true;
        return orgId.toString().equals(principal.getOrganizationId());
    }

    /**
     * Aislamiento: el usuario objetivo debe pertenecer a ESTA organización. Devuelve el
     * usuario con el rol que tiene en ella, o null si no es miembro.
     */
    private User memberOf(UUID userId, UUID orgId) {
        return userUseCase.findMembers(orgId).stream()
                .filter(u -> userId.equals(u.getId()))
                .findFirst()
                .orElse(null);
    }

    public record MemberResponse(UUID id, String email, String name, String pictureUrl, String role,
                                 UUID organizationId, boolean active, java.time.LocalDateTime createdAt) {
    }
}
