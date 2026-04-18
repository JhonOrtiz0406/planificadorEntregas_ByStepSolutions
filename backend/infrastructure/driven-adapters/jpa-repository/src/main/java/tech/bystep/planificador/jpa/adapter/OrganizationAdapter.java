package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.OrganizationEntity;
import tech.bystep.planificador.jpa.repository.OrganizationJpaRepository;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.gateways.OrganizationGateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrganizationAdapter implements OrganizationGateway {

    private final OrganizationJpaRepository repository;

    @Override
    public Organization save(Organization organization) {
        return toModel(repository.save(toEntity(organization)));
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public Optional<Organization> findBySlug(String slug) {
        return repository.findBySlug(slug).map(this::toModel);
    }

    @Override
    public Optional<Organization> findByAdminEmail(String email) {
        return repository.findByAdminEmail(email).map(this::toModel);
    }

    @Override
    public List<Organization> findAll() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlug(slug);
    }

    private Organization toModel(OrganizationEntity e) {
        return Organization.builder()
                .id(e.getId()).name(e.getName()).slug(e.getSlug())
                .logoUrl(e.getLogoUrl()).iconName(e.getIconName()).adminEmail(e.getAdminEmail())
                .active(e.isActive()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private OrganizationEntity toEntity(Organization m) {
        return OrganizationEntity.builder()
                .id(m.getId()).name(m.getName()).slug(m.getSlug())
                .logoUrl(m.getLogoUrl()).iconName(m.getIconName()).adminEmail(m.getAdminEmail())
                .active(m.isActive()).createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt())
                .build();
    }
}
