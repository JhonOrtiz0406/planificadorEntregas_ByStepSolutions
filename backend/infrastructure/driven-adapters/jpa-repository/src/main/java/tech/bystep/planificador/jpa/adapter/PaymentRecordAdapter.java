package tech.bystep.planificador.jpa.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.jpa.entity.PaymentRecordEntity;
import tech.bystep.planificador.jpa.repository.PaymentRecordJpaRepository;
import tech.bystep.planificador.model.PaymentRecord;
import tech.bystep.planificador.model.gateways.PaymentRecordGateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PaymentRecordAdapter implements PaymentRecordGateway {

    private final PaymentRecordJpaRepository repository;

    @Override
    public PaymentRecord save(PaymentRecord record) {
        return toModel(repository.save(toEntity(record)));
    }

    @Override
    public List<PaymentRecord> findByOrderId(UUID orderId) {
        return repository.findByOrderIdOrderByPaymentDateDesc(orderId)
                .stream().map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public Optional<PaymentRecord> findById(UUID id) {
        return repository.findById(id).map(this::toModel);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public BigDecimal sumAmountByOrderId(UUID orderId) {
        return repository.sumAmountByOrderId(orderId);
    }

    private PaymentRecord toModel(PaymentRecordEntity e) {
        return PaymentRecord.builder()
                .id(e.getId()).orderId(e.getOrderId()).amount(e.getAmount())
                .paymentDate(e.getPaymentDate()).paymentMethod(e.getPaymentMethod())
                .notes(e.getNotes()).createdAt(e.getCreatedAt())
                .build();
    }

    private PaymentRecordEntity toEntity(PaymentRecord m) {
        return PaymentRecordEntity.builder()
                .id(m.getId()).orderId(m.getOrderId()).amount(m.getAmount())
                .paymentDate(m.getPaymentDate()).paymentMethod(m.getPaymentMethod())
                .notes(m.getNotes()).createdAt(m.getCreatedAt())
                .build();
    }
}
