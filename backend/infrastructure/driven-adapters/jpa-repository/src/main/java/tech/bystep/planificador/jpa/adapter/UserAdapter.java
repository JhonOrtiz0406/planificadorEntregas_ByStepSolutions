package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.UserEntity;
import tech.bystep.planificador.jpa.repository.UserJpaRepository;
import tech.bystep.planificador.model.User;
import tech.bystep.planificador.model.UserRole;
import tech.bystep.planificador.model.gateways.UserGateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserAdapter implements UserGateway {

    private final UserJpaRepository repository;

    @Override
    public User save(User user) {
        return toModel(repository.save(toEntity(user)));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toModel);
    }

    @Override
    public Optional<User> findByGoogleId(String googleId) {
        return repository.findByGoogleId(googleId).map(this::toModel);
    }

    @Override
    public List<User> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationId(organizationId).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<User> findByOrganizationIdAndRole(UUID organizationId, UserRole role) {
        return repository.findByOrganizationIdAndRole(organizationId, role).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    private User toModel(UserEntity e) {
        return User.builder()
                .id(e.getId()).googleId(e.getGoogleId()).email(e.getEmail())
                .name(e.getName()).pictureUrl(e.getPictureUrl()).role(e.getRole())
                .organizationId(e.getOrganizationId()).fcmToken(e.getFcmToken())
                .active(e.isActive()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private UserEntity toEntity(User m) {
        return UserEntity.builder()
                .id(m.getId()).googleId(m.getGoogleId()).email(m.getEmail())
                .name(m.getName()).pictureUrl(m.getPictureUrl()).role(m.getRole())
                .organizationId(m.getOrganizationId()).fcmToken(m.getFcmToken())
                .active(m.isActive()).createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt())
                .build();
    }
}
