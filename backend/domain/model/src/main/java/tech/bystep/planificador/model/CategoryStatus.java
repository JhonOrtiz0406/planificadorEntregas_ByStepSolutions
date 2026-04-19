package tech.bystep.planificador.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatus {

    private UUID id;
    private String categoryId;
    private String statusKey;
    private String label;
    private int displayOrder;
    private boolean isFinal;
}
