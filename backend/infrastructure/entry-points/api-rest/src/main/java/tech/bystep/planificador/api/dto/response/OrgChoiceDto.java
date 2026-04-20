package tech.bystep.planificador.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgChoiceDto {
    private String id;
    private String name;
    private String category;
    private String iconName;
    private String userRole;
}
