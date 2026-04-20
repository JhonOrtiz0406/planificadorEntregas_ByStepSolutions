package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.UserOrgEntity;
import tech.bystep.planificador.jpa.repository.UserOrgJpaRepository;
import tech.bystep.planificador.model.UserOrganization;
import tech.bystep.planificador.model.gateways.UserOrgGateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserOrgAdapter implements UserOrgGateway {

    private final UserOrgJpaRepository repository;

    @Override
    public void save(UUID userId, UUID organizationId, String role) {
        UserOrgEntity entity = UserOrgEntity.builder()
                .userId(userId)
                .organizationId(organizationId)
                .role(role)
                .joinedAt(OffsetDateTime.now())
                .build();
        repository.save(entity);
    }

    @Override
    public List<UserOrganization> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(e -> UserOrganization.builder()
                        .userId(e.getUserId())
                        .organizationId(e.getOrganizationId())
                        .role(e.getRole())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId) {
        repository.deleteByUserIdAndOrganizationId(userId, organizationId);
    }

    @Override
    public void deleteByOrganizationId(UUID organizationId) {
        repository.deleteByOrganizationId(organizationId);
    }
}
