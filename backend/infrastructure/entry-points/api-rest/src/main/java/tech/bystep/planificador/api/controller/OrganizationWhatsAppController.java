package tech.bystep.planificador.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.bystep.planificador.api.RateLimitService;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppSendResult;
import tech.bystep.planificador.model.whatsapp.WhatsAppTemplateState;
import tech.bystep.planificador.security.UserPrincipal;
import tech.bystep.planificador.usecase.NotificationText;
import tech.bystep.planificador.usecase.WhatsAppAdminUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Módulo WhatsApp de UNA organización. El PLATFORM_ADMIN administra cualquier
 * organización; el ORG_ADMIN solo la suya y solo lo operativo (eventos, prueba, historial).
 * Los tokens nunca salen en las respuestas.
 */
@RestController
@RequestMapping("/api/organizations/{id}/whatsapp")
@RequiredArgsConstructor
public class OrganizationWhatsAppController {

    private final WhatsAppAdminUseCase whatsAppAdminUseCase;
    private final RateLimitService rateLimitService;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record CredentialsRequest(String wabaId, String phoneNumberId, String accessToken,
                                     String supportContactText) {
    }

    public record EnabledRequest(Boolean enabled) {
    }

    public record EventSettingsRequest(Map<String, Boolean> events) {
    }

    public record TestRequest(String phone) {
    }

    public record ConfigResponse(boolean enabled, String status, String wabaId, String phoneNumberId,
                                 String displayPhoneNumber, String verifiedName, boolean hasAccessToken,
                                 String supportContactText, String qualityRating, String lastError,
                                 LocalDateTime lastVerifiedAt) {
    }

    public record OverviewResponse(String organizationPhone, ConfigResponse config,
                                   List<WhatsAppAdminUseCase.EventView> events, Map<String, Long> monthCounts) {
    }

    public record TemplateResponse(String key, String templateName, String status, String rejectionReason) {
    }

    public record MessageResponse(UUID id, String eventKey, String entityType, UUID entityId, String toPhone,
                                  String templateName, String status, String skipReason, String errorCode,
                                  String errorMessage, int attempts, LocalDateTime createdAt, LocalDateTime sentAt) {
    }

    public record TestResponse(boolean success, String errorCode, String errorMessage) {
    }

    // ── Lectura (ORG_ADMIN de esa org o PLATFORM_ADMIN) ───────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OverviewResponse>> overview(@PathVariable("id") UUID id,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccess(principal, id)) return forbidden();
        WhatsAppAdminUseCase.Overview o = whatsAppAdminUseCase.getOverview(id);
        return ResponseEntity.ok(ApiResponse.ok(new OverviewResponse(o.organizationPhone(), toConfig(o.config()),
                o.events(), o.monthCounts())));
    }

    @GetMapping("/messages")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> messages(@PathVariable("id") UUID id,
                                                                      @RequestParam(value = "limit", defaultValue = "20") int limit,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccess(principal, id)) return forbidden();
        List<MessageResponse> list = whatsAppAdminUseCase.recentMessages(id, limit).stream()
                .map(this::toMessage).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PutMapping("/events")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OverviewResponse>> updateEvents(@PathVariable("id") UUID id,
                                                                      @RequestBody EventSettingsRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccess(principal, id)) return forbidden();
        whatsAppAdminUseCase.updateEventSettings(id, request.events());
        return overview(id, principal);
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<TestResponse>> sendTest(@PathVariable("id") UUID id,
                                                              @RequestBody TestRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccess(principal, id)) return forbidden();
        if (!rateLimitService.isAllowed("wa-test:" + id, 10, 3_600_000L)) {
            return ResponseEntity.status(429).body(ApiResponse.error("Demasiadas pruebas. Intenta en una hora."));
        }
        WhatsAppSendResult r = whatsAppAdminUseCase.sendTest(id, request.phone());
        return ResponseEntity.ok(ApiResponse.ok(new TestResponse(r.success(), r.errorCode(), r.errorMessage())));
    }

    @PostMapping("/templates/refresh")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORG_ADMIN')")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> refreshTemplates(@PathVariable("id") UUID id,
                                                                                @AuthenticationPrincipal UserPrincipal principal) {
        if (!canAccess(principal, id)) return forbidden();
        return ResponseEntity.ok(ApiResponse.ok(toTemplates(whatsAppAdminUseCase.refreshTemplates(id))));
    }

    // ── Administración (solo PLATFORM_ADMIN) ─────────────────────────────────

    @PutMapping("/credentials")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<ConfigResponse>> saveCredentials(@PathVariable("id") UUID id,
                                                                       @RequestBody CredentialsRequest request) {
        WhatsAppConfig config = whatsAppAdminUseCase.saveCredentials(id, request.wabaId(), request.phoneNumberId(),
                request.accessToken(), request.supportContactText());
        return ResponseEntity.ok(ApiResponse.ok("Credenciales guardadas", toConfig(config)));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<ConfigResponse>> verify(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(toConfig(whatsAppAdminUseCase.verify(id))));
    }

    @PatchMapping("/enabled")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<ConfigResponse>> setEnabled(@PathVariable("id") UUID id,
                                                                  @RequestBody EnabledRequest request) {
        boolean enabled = request.enabled() != null && request.enabled();
        return ResponseEntity.ok(ApiResponse.ok(toConfig(whatsAppAdminUseCase.setEnabled(id, enabled))));
    }

    @PostMapping("/templates/sync")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> syncTemplates(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(toTemplates(whatsAppAdminUseCase.syncTemplates(id))));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean canAccess(UserPrincipal principal, UUID orgId) {
        if (principal == null) return false;
        if ("PLATFORM_ADMIN".equals(principal.getRole())) return true;
        return orgId.toString().equals(principal.getOrganizationId());
    }

    private static <T> ResponseEntity<ApiResponse<T>> forbidden() {
        return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
    }

    private static ConfigResponse toConfig(WhatsAppConfig c) {
        if (c == null) return null;
        return new ConfigResponse(c.isEnabled(), c.getStatus() != null ? c.getStatus().name() : null,
                c.getWabaId(), c.getPhoneNumberId(), c.getDisplayPhoneNumber(), c.getVerifiedName(),
                c.getAccessToken() != null && !c.getAccessToken().isBlank(),
                c.getSupportContactText(), c.getQualityRating(), c.getLastError(), c.getLastVerifiedAt());
    }

    private static List<TemplateResponse> toTemplates(List<WhatsAppTemplateState> states) {
        return states.stream()
                .map(t -> new TemplateResponse(t.getTemplateKey(), t.getTemplateName(), t.getStatus(),
                        t.getRejectionReason()))
                .toList();
    }

    private MessageResponse toMessage(WhatsAppMessage m) {
        return new MessageResponse(m.getId(), m.getEventKey(), m.getEntityType(), m.getEntityId(),
                NotificationText.maskPhone(m.getToPhone()), m.getTemplateName(),
                m.getStatus() != null ? m.getStatus().name() : null, m.getSkipReason(), m.getErrorCode(),
                m.getErrorMessage(), m.getAttempts(), m.getCreatedAt(), m.getSentAt());
    }
}
