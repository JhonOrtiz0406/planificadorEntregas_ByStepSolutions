package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.UserOrgEntity;
import tech.bystep.planificador.jpa.repository.UserOrgJpaRepository;
import tech.bystep.planificador.model.UserOrganization;
import tech.bystep.planificador.model.gateways.UserOrgGateway;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserOrgAdapter implements UserOrgGateway {

    private final UserOrgJpaRepository repository;

    @Override
    public void save(UUID userId, UUID organizationId, String role) {
        UserOrgEntity entity = repository.findById(new UserOrgEntity.UserOrgId(userId, organizationId))
                .orElseGet(() -> UserOrgEntity.builder()
                        .userId(userId)
                        .organizationId(organizationId)
                        .joinedAt(OffsetDateTime.now())
                        .build());
        entity.setRole(role);
        entity.setActive(true);
        repository.save(entity);
    }

    @Override
    public List<UserOrganization> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<UserOrganization> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationId(organizationId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public Optional<UserOrganization> find(UUID userId, UUID organizationId) {
        return repository.findById(new UserOrgEntity.UserOrgId(userId, organizationId)).map(this::toModel);
    }

    @Override
    public void setActive(UUID userId, UUID organizationId, boolean active) {
        repository.updateActive(userId, organizationId, active);
    }

    @Override
    public void setActiveForOrganization(UUID organizationId, boolean active) {
        repository.updateActiveForOrganization(organizationId, active);
    }

    @Override
    public void deleteByUserIdAndOrganizationId(UUID userId, UUID organizationId) {
        repository.deleteByUserIdAndOrganizationId(userId, organizationId);
    }

    @Override
    public void deleteByOrganizationId(UUID organizationId) {
        repository.deleteByOrganizationId(organizationId);
    }

    private UserOrganization toModel(UserOrgEntity e) {
        return UserOrganization.builder()
                .userId(e.getUserId())
                .organizationId(e.getOrganizationId())
                .role(e.getRole())
                .active(e.isActive())
                .build();
    }
}
