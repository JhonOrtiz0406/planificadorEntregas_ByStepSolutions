package tech.bystep.planificador.usecase;

import lombok.RequiredArgsConstructor;
import tech.bystep.planificador.model.PaymentStatus;
import tech.bystep.planificador.model.Repair;
import tech.bystep.planificador.model.RepairPayment;
import tech.bystep.planificador.model.RepairStatus;
import tech.bystep.planificador.model.gateways.OrganizationGateway;
import tech.bystep.planificador.model.gateways.RepairGateway;
import tech.bystep.planificador.model.gateways.RepairPaymentGateway;
import tech.bystep.planificador.model.gateways.StorageGateway;
import tech.bystep.planificador.model.whatsapp.NotificationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class RepairUseCase {

    private static final int MAX_PHOTOS = 3;

    private final RepairGateway repairGateway;
    private final RepairPaymentGateway repairPaymentGateway;
    private final OrganizationGateway organizationGateway;
    private final StorageGateway storageGateway;
    private final ClientNotificationUseCase clientNotifications;

    public Repair create(Repair repair) {
        assertJewelryOrganization(repair.getOrganizationId());
        assertPhotoLimit(repair.getPhotoUrls());

        if (repair.getEntryDate() == null) repair.setEntryDate(LocalDate.now());
        repair.setRepairStatus(RepairStatus.RECEIVED);
        repair.setPaymentStatus(PaymentStatus.UNPAID);
        repair.setPaymentAmount(BigDecimal.ZERO);
        if (repair.getNotifyWhatsapp() == null) repair.setNotifyWhatsapp(true);
        repair.setCreatedAt(LocalDateTime.now());
        repair.setUpdatedAt(LocalDateTime.now());
        Repair saved = repairGateway.save(repair);
        notifyClient(saved, NotificationEvent.REPAIR_RECEIVED,
                Arrays.asList(clientName(saved), item(saved), NotificationText.date(saved.getEntryDate()),
                        NotificationText.date(saved.getDeliveryDate())),
                "REPAIR:" + saved.getId() + ":CREATED");
        return saved;
    }

    public Repair update(UUID id, UUID organizationId, Repair updates) {
        Repair existing = repairGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + id));

        if (updates.getClientFirstName() != null) existing.setClientFirstName(updates.getClientFirstName());
        if (updates.getClientLastName() != null) existing.setClientLastName(updates.getClientLastName());
        if (updates.getClientPhone() != null) existing.setClientPhone(updates.getClientPhone());
        if (updates.getItemDescription() != null) existing.setItemDescription(updates.getItemDescription());
        if (updates.getRepairDescription() != null) existing.setRepairDescription(updates.getRepairDescription());
        if (updates.getTotalPrice() != null) existing.setTotalPrice(updates.getTotalPrice());
        if (updates.getEntryDate() != null) existing.setEntryDate(updates.getEntryDate());
        if (updates.getDeliveryDate() != null) existing.setDeliveryDate(updates.getDeliveryDate());
        if (updates.getNotifyWhatsapp() != null) existing.setNotifyWhatsapp(updates.getNotifyWhatsapp());
        if (updates.getPhotoUrls() != null) {
            assertPhotoLimit(updates.getPhotoUrls());
            existing.setPhotoUrls(updates.getPhotoUrls());
        }

        recalculatePaymentStatus(existing);
        existing.setUpdatedAt(LocalDateTime.now());
        return repairGateway.save(existing);
    }

    public Repair removePhoto(UUID id, UUID organizationId, String photoUrl) {
        Repair repair = repairGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + id));
        List<String> photos = repair.getPhotoUrls() != null
                ? new ArrayList<>(repair.getPhotoUrls()) : new ArrayList<>();
        if (!photos.remove(photoUrl)) {
            // Aislamiento: nunca borrar archivos que no pertenecen a este arreglo (ni a esta organización).
            throw new IllegalArgumentException("La foto no pertenece a este arreglo");
        }
        repair.setPhotoUrls(photos);
        repair.setUpdatedAt(LocalDateTime.now());
        Repair saved = repairGateway.save(repair);
        storageGateway.deleteFile(photoUrl);
        return saved;
    }

    public Repair updateStatus(UUID id, UUID organizationId, RepairStatus status) {
        Repair repair = repairGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + id));
        repair.setRepairStatus(status);
        repair.setUpdatedAt(LocalDateTime.now());
        Repair saved = repairGateway.save(repair);
        if (status == RepairStatus.READY_TO_DELIVER) {
            notifyClient(saved, NotificationEvent.REPAIR_READY,
                    Arrays.asList(clientName(saved), item(saved), NotificationText.money(balance(saved))),
                    "REPAIR:" + saved.getId() + ":STATUS:" + status.name());
        } else if (status == RepairStatus.DELIVERED) {
            notifyClient(saved, NotificationEvent.REPAIR_DELIVERED,
                    Arrays.asList(clientName(saved), item(saved)),
                    "REPAIR:" + saved.getId() + ":STATUS:" + status.name());
        }
        return saved;
    }

    public RepairPayment addPayment(UUID repairId, UUID organizationId, BigDecimal amount,
                                     LocalDate paymentDate, String paymentMethod, String notes,
                                     UUID createdById) {
        Repair repair = repairGateway.findByIdAndOrganizationId(repairId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + repairId));

        BigDecimal total = repair.getTotalPrice() != null ? repair.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal alreadyPaid = repairPaymentGateway.sumAmountByRepairId(repairId);
        BigDecimal balance = total.subtract(alreadyPaid);
        if (amount.compareTo(balance) > 0) {
            throw new IllegalStateException(
                    "El abono no puede superar el saldo pendiente (" + balance + ")");
        }

        RepairPayment payment = RepairPayment.builder()
                .repairId(repairId).amount(amount).paymentDate(paymentDate)
                .paymentMethod(paymentMethod).notes(notes).createdById(createdById)
                .createdAt(LocalDateTime.now())
                .build();
        RepairPayment saved = repairPaymentGateway.save(payment);

        applyPaymentTotals(repair);
        notifyClient(repair, NotificationEvent.REPAIR_PAYMENT,
                Arrays.asList(clientName(repair), NotificationText.money(amount), item(repair),
                        NotificationText.money(balance(repair))),
                "REPAIR_PAYMENT:" + saved.getId());
        return saved;
    }

    public List<RepairPayment> getPayments(UUID repairId, UUID organizationId) {
        repairGateway.findByIdAndOrganizationId(repairId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + repairId));
        return repairPaymentGateway.findByRepairId(repairId);
    }

    public void deletePayment(UUID repairId, UUID organizationId, UUID paymentId) {
        Repair repair = repairGateway.findByIdAndOrganizationId(repairId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + repairId));
        repairPaymentGateway.findById(paymentId)
                // Aislamiento: el abono debe pertenecer a ESTE arreglo (que ya se validó que es de la organización).
                .filter(p -> repairId.equals(p.getRepairId()))
                .orElseThrow(() -> new IllegalArgumentException("Abono no encontrado: " + paymentId));
        repairPaymentGateway.deleteById(paymentId);
        applyPaymentTotals(repair);
    }

    public Optional<Repair> findById(UUID id, UUID organizationId) {
        return repairGateway.findByIdAndOrganizationId(id, organizationId);
    }

    public List<Repair> findAllByOrganization(UUID organizationId) {
        return repairGateway.findByOrganizationIdOrderByEntryDateDesc(organizationId);
    }

    public void delete(UUID id, UUID organizationId) {
        repairGateway.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Arreglo no encontrado: " + id));
        repairGateway.deleteById(id);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void notifyClient(Repair repair, NotificationEvent event, List<String> params, String idempotencyKey) {
        clientNotifications.notify(repair.getOrganizationId(), event, ClientNotificationUseCase.ENTITY_REPAIR,
                repair.getId(), repair.getClientPhone(), repair.getNotifyWhatsapp(), params, idempotencyKey);
    }

    private static String clientName(Repair repair) {
        String first = repair.getClientFirstName();
        return first != null && !first.isBlank() ? first.trim() : repair.clientFullName().trim();
    }

    private static String item(Repair repair) {
        return NotificationText.shortText(repair.getItemDescription(), 60);
    }

    private static BigDecimal balance(Repair repair) {
        BigDecimal total = repair.getTotalPrice() != null ? repair.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal paid = repair.getPaymentAmount() != null ? repair.getPaymentAmount() : BigDecimal.ZERO;
        BigDecimal balance = total.subtract(paid);
        return balance.signum() < 0 ? BigDecimal.ZERO : balance;
    }

    private void applyPaymentTotals(Repair repair) {
        BigDecimal total = repairPaymentGateway.sumAmountByRepairId(repair.getId());
        repair.setPaymentAmount(total);
        repair.setUpdatedAt(LocalDateTime.now());
        recalculatePaymentStatus(repair);
        repairGateway.save(repair);
    }

    private Repair recalculatePaymentStatus(Repair repair) {
        BigDecimal total = repair.getTotalPrice() != null ? repair.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal paid = repair.getPaymentAmount() != null ? repair.getPaymentAmount() : BigDecimal.ZERO;
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            repair.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (paid.compareTo(total) >= 0) {
            repair.setPaymentStatus(PaymentStatus.PAID);
        } else {
            repair.setPaymentStatus(PaymentStatus.PARTIAL);
        }
        return repair;
    }

    private void assertPhotoLimit(List<String> photoUrls) {
        if (photoUrls != null && photoUrls.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Máximo " + MAX_PHOTOS + " fotos por arreglo");
        }
    }

    private void assertJewelryOrganization(UUID organizationId) {
        var org = organizationGateway.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organización no encontrada: " + organizationId));
        if (!"JEWELRY".equalsIgnoreCase(org.getCategory())) {
            throw new IllegalStateException("El módulo de Arreglos solo está disponible para organizaciones de tipo Joyería");
        }
    }
}
