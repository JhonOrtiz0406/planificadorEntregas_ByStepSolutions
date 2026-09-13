package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.RepairEntity;
import tech.bystep.planificador.jpa.repository.RepairJpaRepository;
import tech.bystep.planificador.model.Repair;
import tech.bystep.planificador.model.gateways.RepairGateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RepairAdapter implements RepairGateway {

    private final RepairJpaRepository repository;

    @Override
    public Repair save(Repair repair) {
        return toModel(repository.save(toEntity(repair)));
    }

    @Override
    public Optional<Repair> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public Optional<Repair> findByIdAndOrganizationId(UUID id, UUID organizationId) {
        return repository.findByIdAndOrganizationId(id, organizationId).map(this::toModel);
    }

    @Override
    public List<Repair> findByOrganizationIdOrderByEntryDateDesc(UUID organizationId) {
        return repository.findByOrganizationIdOrderByEntryDateDesc(organizationId)
                .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Repair toModel(RepairEntity e) {
        return Repair.builder()
                .id(e.getId()).clientFirstName(e.getClientFirstName()).clientLastName(e.getClientLastName())
                .clientPhone(e.getClientPhone()).itemDescription(e.getItemDescription())
                .repairDescription(e.getRepairDescription())
                .photoUrls(e.getPhotoUrls() != null ? e.getPhotoUrls() : new java.util.ArrayList<>())
                .entryDate(e.getEntryDate()).deliveryDate(e.getDeliveryDate())
                .repairStatus(e.getRepairStatus()).paymentStatus(e.getPaymentStatus())
                .totalPrice(e.getTotalPrice()).paymentAmount(e.getPaymentAmount())
                .organizationId(e.getOrganizationId()).createdById(e.getCreatedById())
                .notifyWhatsapp(e.isNotifyWhatsapp())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private RepairEntity toEntity(Repair m) {
        return RepairEntity.builder()
                .id(m.getId()).clientFirstName(m.getClientFirstName()).clientLastName(m.getClientLastName())
                .clientPhone(m.getClientPhone()).itemDescription(m.getItemDescription())
                .repairDescription(m.getRepairDescription())
                .photoUrls(m.getPhotoUrls() != null ? m.getPhotoUrls() : new java.util.ArrayList<>())
                .entryDate(m.getEntryDate()).deliveryDate(m.getDeliveryDate())
                .repairStatus(m.getRepairStatus()).paymentStatus(m.getPaymentStatus())
                .totalPrice(m.getTotalPrice()).paymentAmount(m.getPaymentAmount())
                .organizationId(m.getOrganizationId()).createdById(m.getCreatedById())
                .notifyWhatsapp(m.getNotifyWhatsapp() == null || m.getNotifyWhatsapp())
                .createdAt(m.getCreatedAt()).updatedAt(m.getUpdatedAt())
                .build();
    }
}
