package tech.bystep.planificador.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private UserResponse user;
    private boolean requiresOrgSelection;
    private boolean noOrgAccess;
    private String selectionToken;
    private List<OrgChoiceDto> availableOrgs;
}
