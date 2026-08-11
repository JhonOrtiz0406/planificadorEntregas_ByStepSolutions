package tech.bystep.planificador.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.bystep.planificador.model.RepairStatus;

@Data
public class UpdateRepairStatusRequest {
    @NotNull
    private RepairStatus repairStatus;
}
