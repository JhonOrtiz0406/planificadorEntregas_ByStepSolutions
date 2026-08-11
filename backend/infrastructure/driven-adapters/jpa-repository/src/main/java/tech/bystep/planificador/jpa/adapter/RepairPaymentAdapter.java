package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.RepairPaymentEntity;
import tech.bystep.planificador.jpa.repository.RepairPaymentJpaRepository;
import tech.bystep.planificador.model.RepairPayment;
import tech.bystep.planificador.model.gateways.RepairPaymentGateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RepairPaymentAdapter implements RepairPaymentGateway {

    private final RepairPaymentJpaRepository repository;

    @Override
    public RepairPayment save(RepairPayment payment) {
        return toModel(repository.save(toEntity(payment)));
    }

    @Override
    public List<RepairPayment> findByRepairId(UUID repairId) {
        return repository.findByRepairIdOrderByPaymentDateDesc(repairId)
                .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public Optional<RepairPayment> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public BigDecimal sumAmountByRepairId(UUID repairId) {
        return repository.sumAmountByRepairId(repairId);
    }

    private RepairPayment toModel(RepairPaymentEntity e) {
        return RepairPayment.builder()
                .id(e.getId()).repairId(e.getRepairId()).amount(e.getAmount())
                .paymentDate(e.getPaymentDate()).paymentMethod(e.getPaymentMethod())
                .notes(e.getNotes()).createdById(e.getCreatedById()).createdAt(e.getCreatedAt())
                .build();
    }

    private RepairPaymentEntity toEntity(RepairPayment m) {
        return RepairPaymentEntity.builder()
                .id(m.getId()).repairId(m.getRepairId()).amount(m.getAmount())
                .paymentDate(m.getPaymentDate()).paymentMethod(m.getPaymentMethod())
                .notes(m.getNotes()).createdById(m.getCreatedById()).createdAt(m.getCreatedAt())
                .build();
    }
}
