package tech.bystep.planificador.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class Repair {

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
    private UUID organizationId;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String clientFullName() {
        return (clientFirstName == null ? "" : clientFirstName)
                + " "
                + (clientLastName == null ? "" : clientLastName);
    }
}
