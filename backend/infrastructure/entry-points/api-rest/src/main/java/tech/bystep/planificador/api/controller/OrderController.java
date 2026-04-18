package tech.bystep.planificador.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.bystep.planificador.api.dto.request.CreateOrderRequest;
import tech.bystep.planificador.api.dto.request.UpdateOrderStatusRequest;
import tech.bystep.planificador.api.dto.response.ApiResponse;
import tech.bystep.planificador.api.dto.response.OrderResponse;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<OrderResponse> orders = orderUseCase.findByDateRange(start, end)
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
                .deliveryDate(request.getDeliveryDate())
                .totalPrice(request.getTotalPrice())
                .organizationId(orgId)
                .createdById(userId)
                .build();
        Order created = orderUseCase.create(order);
        return ResponseEntity.status(201).body(ApiResponse.ok("Order created", toResponse(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable("id") UUID id,
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID orgId = UUID.fromString(principal.getOrganizationId());
        Order updates = Order.builder()
                .productName(request.getProductName())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientAddress(request.getClientAddress())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
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
        }
        return OrderResponse.builder()
                .id(order.getId()).orderNumber(order.getOrderNumber()).productName(order.getProductName())
                .clientName(order.getClientName()).clientPhone(order.getClientPhone())
                .clientAddress(order.getClientAddress()).description(order.getDescription())
                .photoUrl(order.getPhotoUrl()).deliveryDate(order.getDeliveryDate())
                .progressStatus(order.getProgressStatus()).paymentStatus(order.getPaymentStatus())
                .paymentAmount(order.getPaymentAmount()).totalPrice(order.getTotalPrice())
                .balanceDue(balance).organizationId(order.getOrganizationId())
                .daysUntilDelivery(order.daysUntilDelivery()).overdue(order.isOverdue())
                .createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt())
                .build();
    }
}
