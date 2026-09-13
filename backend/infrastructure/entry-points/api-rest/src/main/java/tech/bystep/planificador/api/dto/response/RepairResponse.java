package tech.bystep.planificador.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class RepairResponse {
    private UUID id;
    private String clientFirstName;
    private String clientLastName;
    private String clientPhone;
    private String itemDescription;
    private String repairDescription;
    private List<String> photoUrls = new ArrayList<>();
    private LocalDate entryDate;
    private LocalDate deliveryDate;
    private RepairStatus repairStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalPrice;
    private BigDecimal paymentAmount;
    private BigDecimal balanceDue;
    private UUID organizationId;
    private Boolean notifyWhatsapp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
