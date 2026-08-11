package tech.bystep.planificador.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.request.AddRepairPaymentRequest;
import tech.bystep.planificador.api.dto.request.CreateRepairRequest;
import tech.bystep.planificador.api.dto.request.UpdateRepairRequest;
import tech.bystep.planificador.api.dto.request.UpdateRepairStatusRequest;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.api.dto.response.RepairPaymentResponse;
import tech.bystep.planificador.api.dto.response.RepairResponse;
import tech.bystep.planificador.model.Repair;
import tech.bystep.planificador.model.RepairPayment;
import tech.bystep.planificador.security.UserPrincipal;
import tech.bystep.planificador.usecase.RepairUseCase;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
public class RepairController {

    private final RepairUseCase repairUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<RepairResponse>>> getAllRepairs(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        List<RepairResponse> repairs = repairUseCase.findAllByOrganization(orgId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(repairs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RepairResponse>> getRepair(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        return repairUseCase.findById(id, orgId)
                .map(r -> ResponseEntity.ok(ApiResponse.ok(toResponse(r))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RepairResponse>> createRepair(
            @Valid @RequestBody CreateRepairRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        UUID userId = UUID.fromString(principal.getUserId());
        Repair repair = Repair.builder()
                .clientFirstName(request.getClientFirstName())
                .clientLastName(request.getClientLastName())
                .clientPhone(request.getClientPhone())
                .itemDescription(request.getItemDescription())
                .repairDescription(request.getRepairDescription())
                .totalPrice(request.getTotalPrice())
                .entryDate(request.getEntryDate())
                .deliveryDate(request.getDeliveryDate())
                .photoUrls(request.getPhotoUrls() != null ? request.getPhotoUrls() : new java.util.ArrayList<>())
                .organizationId(orgId)
                .createdById(userId)
                .build();
        Repair created = repairUseCase.create(repair);
        return ResponseEntity.status(201).body(ApiResponse.ok("Arreglo creado", toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RepairResponse>> updateRepair(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRepairRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        Repair updates = Repair.builder()
                .clientFirstName(request.getClientFirstName())
                .clientLastName(request.getClientLastName())
                .clientPhone(request.getClientPhone())
                .itemDescription(request.getItemDescription())
                .repairDescription(request.getRepairDescription())
                .totalPrice(request.getTotalPrice())
                .entryDate(request.getEntryDate())
                .deliveryDate(request.getDeliveryDate())
                .photoUrls(request.getPhotoUrls())
                .build();
        Repair updated = repairUseCase.update(id, orgId, updates);
        return ResponseEntity.ok(ApiResponse.ok("Arreglo actualizado", toResponse(updated)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RepairResponse>> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRepairStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        Repair updated = repairUseCase.updateStatus(id, orgId, request.getRepairStatus());
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado", toResponse(updated)));
    }

    @DeleteMapping("/{id}/photos")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RepairResponse>> deletePhoto(
            @PathVariable("id") UUID id,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        String photoUrl = body.get("url");
        if (photoUrl == null || photoUrl.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("url is required"));
        }
        Repair updated = repairUseCase.removePhoto(id, orgId, photoUrl);
        return ResponseEntity.ok(ApiResponse.ok("Foto eliminada", toResponse(updated)));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<RepairPaymentResponse>> addPayment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddRepairPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        UUID userId = UUID.fromString(principal.getUserId());
        RepairPayment payment = repairUseCase.addPayment(id, orgId, request.getAmount(),
                request.getPaymentDate(), request.getPaymentMethod(), request.getNotes(), userId);
        return ResponseEntity.status(201).body(ApiResponse.ok("Abono registrado", toPaymentResponse(payment)));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<RepairPaymentResponse>>> getPayments(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        List<RepairPaymentResponse> payments = repairUseCase.getPayments(id, orgId)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(
            @PathVariable("id") UUID id,
            @PathVariable("paymentId") UUID paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        repairUseCase.deletePayment(id, orgId, paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Abono eliminado", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRepair(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        repairUseCase.delete(id, orgId);
        return ResponseEntity.ok(ApiResponse.ok("Arreglo eliminado", null));
    }

    private RepairResponse toResponse(Repair repair) {
        BigDecimal balance = BigDecimal.ZERO;
        if (repair.getTotalPrice() != null && repair.getPaymentAmount() != null) {
            balance = repair.getTotalPrice().subtract(repair.getPaymentAmount());
            if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
        }
        return RepairResponse.builder()
                .id(repair.getId())
                .clientFirstName(repair.getClientFirstName()).clientLastName(repair.getClientLastName())
                .clientPhone(repair.getClientPhone())
                .itemDescription(repair.getItemDescription()).repairDescription(repair.getRepairDescription())
                .photoUrls(repair.getPhotoUrls() != null ? repair.getPhotoUrls() : new java.util.ArrayList<>())
                .entryDate(repair.getEntryDate()).deliveryDate(repair.getDeliveryDate())
                .repairStatus(repair.getRepairStatus()).paymentStatus(repair.getPaymentStatus())
                .totalPrice(repair.getTotalPrice()).paymentAmount(repair.getPaymentAmount())
                .balanceDue(balance).organizationId(repair.getOrganizationId())
                .createdAt(repair.getCreatedAt()).updatedAt(repair.getUpdatedAt())
                .build();
    }

    private RepairPaymentResponse toPaymentResponse(RepairPayment p) {
        return RepairPaymentResponse.builder()
                .id(p.getId()).repairId(p.getRepairId()).amount(p.getAmount())
                .paymentDate(p.getPaymentDate()).paymentMethod(p.getPaymentMethod())
                .notes(p.getNotes()).createdAt(p.getCreatedAt())
                .build();
    }
}
