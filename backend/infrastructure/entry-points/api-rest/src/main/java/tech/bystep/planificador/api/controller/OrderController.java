package tech.bystep.planificador.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.request.AddPaymentRecordRequest;
import tech.bystep.planificador.api.dto.request.CreateOrderRequest;
import tech.bystep.planificador.api.dto.request.UpdateOrderRequest;
import tech.bystep.planificador.api.dto.request.UpdateOrderStatusRequest;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.api.dto.response.OrderResponse;
import tech.bystep.planificador.api.dto.response.PaymentRecordResponse;
import tech.bystep.planificador.model.PaymentRecord;
import tech.bystep.planificador.model.Order;
import tech.bystep.planificador.security.UserPrincipal;
import tech.bystep.planificador.usecase.OrderUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE','ORG_DELIVERY')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        List<OrderResponse> orders = orderUseCase.findAllByOrganization(orgId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE','ORG_DELIVERY')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingOrders(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        List<OrderResponse> orders = orderUseCase.findPendingDeliveries(orgId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE','ORG_DELIVERY')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getCalendarOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        List<OrderResponse> orders = orderUseCase.findByDateRange(orgId, start, end)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE','ORG_DELIVERY')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        return orderUseCase.findById(id, orgId)
                .map(o -> ResponseEntity.ok(ApiResponse.ok(toResponse(o))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        UUID userId = UUID.fromString(principal.getUserId());
        Order order = Order.builder()
                .productName(request.getProductName())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientAddress(request.getClientAddress())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .photoUrls(request.getPhotoUrls() != null ? request.getPhotoUrls() : new java.util.ArrayList<>())
                .deliveryDate(request.getDeliveryDate())
                .totalPrice(request.getTotalPrice())
                .organizationId(orgId)
                .createdById(userId)
                .build();
        Order created = orderUseCase.create(order);
        return ResponseEntity.status(201).body(ApiResponse.ok("Order created", toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable("id") UUID id,
            @RequestBody UpdateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        Order updates = Order.builder()
                .productName(request.getProductName())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientAddress(request.getClientAddress())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .photoUrls(request.getPhotoUrls())
                .deliveryDate(request.getDeliveryDate())
                .totalPrice(request.getTotalPrice())
                .build();
        Order updated = orderUseCase.update(id, orgId, updates);
        return ResponseEntity.ok(ApiResponse.ok("Order updated", toResponse(updated)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable("id") UUID id,
            @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        Order order = null;
        if (request.getProgressStatus() != null) {
            order = orderUseCase.updateProgressStatus(id, orgId, request.getProgressStatus());
        }
        if (request.getPaymentStatus() != null) {
            order = orderUseCase.updatePaymentStatus(id, orgId, request.getPaymentStatus(), request.getPaymentAmount());
        }
        if (order == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No status provided"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Status updated", toResponse(order)));
    }

    @DeleteMapping("/{id}/photos")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderResponse>> deletePhoto(
            @PathVariable("id") UUID id,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        String photoUrl = body.get("url");
        if (photoUrl == null || photoUrl.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("url is required"));
        }
        Order updated = orderUseCase.removePhoto(id, orgId, photoUrl);
        return ResponseEntity.ok(ApiResponse.ok("Photo deleted", toResponse(updated)));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentRecordResponse>> addPaymentRecord(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddPaymentRecordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        PaymentRecord record = orderUseCase.addPaymentRecord(id, orgId, request.getAmount(),
                request.getPaymentDate(), request.getPaymentMethod(), request.getNotes());
        return ResponseEntity.status(201).body(ApiResponse.ok("Payment record added", toPaymentResponse(record)));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE','ORG_DELIVERY')")
    public ResponseEntity<ApiResponse<List<PaymentRecordResponse>>> getPaymentRecords(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        List<PaymentRecordResponse> records = orderUseCase.getPaymentRecords(id, orgId)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(records));
    }

    @DeleteMapping("/{id}/payments/{recordId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN','ORG_EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deletePaymentRecord(
            @PathVariable("id") UUID id,
            @PathVariable("recordId") UUID recordId,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        orderUseCase.deletePaymentRecord(id, orgId, recordId);
        return ResponseEntity.ok(ApiResponse.ok("Payment record deleted", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        orderUseCase.delete(id, orgId);
        return ResponseEntity.ok(ApiResponse.ok("Order deleted", null));
    }

    private OrderResponse toResponse(Order order) {
        BigDecimal balance = BigDecimal.ZERO;
        if (order.getTotalPrice() != null && order.getPaymentAmount() != null) {
            balance = order.getTotalPrice().subtract(order.getPaymentAmount());
            if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
        }
        return OrderResponse.builder()
                .id(order.getId()).orderNumber(order.getOrderNumber()).productName(order.getProductName())
                .clientName(order.getClientName()).clientPhone(order.getClientPhone())
                .clientAddress(order.getClientAddress()).description(order.getDescription())
                .photoUrl(order.getPhotoUrl())
                .photoUrls(order.getPhotoUrls() != null ? order.getPhotoUrls() : new java.util.ArrayList<>())
                .deliveryDate(order.getDeliveryDate())
                .progressStatus(order.getProgressStatus()).paymentStatus(order.getPaymentStatus())
                .paymentAmount(order.getPaymentAmount()).totalPrice(order.getTotalPrice())
                .balanceDue(balance).organizationId(order.getOrganizationId())
                .daysUntilDelivery(order.daysUntilDelivery()).overdue(order.isOverdue())
                .createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt())
                .build();
    }

    private PaymentRecordResponse toPaymentResponse(PaymentRecord r) {
        return PaymentRecordResponse.builder()
                .id(r.getId()).orderId(r.getOrderId()).amount(r.getAmount())
                .paymentDate(r.getPaymentDate()).paymentMethod(r.getPaymentMethod())
                .notes(r.getNotes()).createdAt(r.getCreatedAt())
                .build();
    }
}
