package tech.bystep.planificador.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tech.bystep.planificador.jpa.converter.StringListConverter;
import tech.bystep.planificador.model.PaymentStatus;
import tech.bystep.planificador.model.RepairStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "repairs")
public class RepairEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_first_name", nullable = false)
    private String clientFirstName;

    @Column(name = "client_last_name", nullable = false)
    private String clientLastName;

    @Column(name = "client_phone", nullable = false)
    private String clientPhone;

    @Column(name = "item_description", columnDefinition = "TEXT", nullable = false)
    private String itemDescription;

    @Column(name = "repair_description", columnDefinition = "TEXT", nullable = false)
    private String repairDescription;

    @Convert(converter = StringListConverter.class)
    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "repair_status", nullable = false)
    private RepairStatus repairStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "payment_amount", precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_by")
    private UUID createdById;

    @Column(name = "notify_whatsapp", nullable = false)
    private boolean notifyWhatsapp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
