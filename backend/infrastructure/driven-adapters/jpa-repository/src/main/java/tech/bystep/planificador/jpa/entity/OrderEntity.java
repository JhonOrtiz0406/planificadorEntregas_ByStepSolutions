package tech.bystep.planificador.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tech.bystep.planificador.model.PaymentStatus;
import tech.bystep.planificador.model.ProgressStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_phone")
    private String clientPhone;

    @Column(name = "client_address")
    private String clientAddress;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status")
    private ProgressStatus progressStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "payment_amount", precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_by")
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
