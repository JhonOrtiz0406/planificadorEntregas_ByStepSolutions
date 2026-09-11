package tech.bystep.planificador.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.bystep.planificador.model.Organization;
import tech.bystep.planificador.model.gateways.NotificationSettingsGateway;
import tech.bystep.planificador.model.gateways.OrganizationGateway;
import tech.bystep.planificador.model.gateways.WhatsAppConfigGateway;
import tech.bystep.planificador.model.gateways.WhatsAppDispatchTrigger;
import tech.bystep.planificador.model.gateways.WhatsAppMessageGateway;
import tech.bystep.planificador.model.gateways.WhatsAppTemplateGateway;
import tech.bystep.planificador.model.whatsapp.NotificationEvent;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppConnectionStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessageStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppTemplateState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientNotificationUseCaseTest {

    private final UUID orgId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private OrganizationGateway organizationGateway;
    private WhatsAppConfigGateway configGateway;
    private WhatsAppTemplateGateway templateGateway;
    private NotificationSettingsGateway settingsGateway;
    private WhatsAppMessageGateway messageGateway;
    private WhatsAppDispatchTrigger trigger;
    private ClientNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        organizationGateway = mock(OrganizationGateway.class);
        configGateway = mock(WhatsAppConfigGateway.class);
        templateGateway = mock(WhatsAppTemplateGateway.class);
        settingsGateway = mock(NotificationSettingsGateway.class);
        messageGateway = mock(WhatsAppMessageGateway.class);
        trigger = mock(WhatsAppDispatchTrigger.class);
        useCase = new ClientNotificationUseCase(organizationGateway, configGateway, templateGateway,
                settingsGateway, messageGateway, trigger);

        when(organizationGateway.findById(orgId)).thenReturn(Optional.of(
                Organization.builder().id(orgId).name("Joyería 1").category("JEWELRY").active(true).build()));
        when(settingsGateway.findByOrganizationId(orgId)).thenReturn(Map.of());
        when(configGateway.findByOrganizationId(orgId)).thenReturn(Optional.of(connectedConfig()));
        when(templateGateway.find(orgId, NotificationEvent.ORDER_CREATED.name(), "es")).thenReturn(Optional.of(
                WhatsAppTemplateState.builder().organizationId(orgId).templateKey("ORDER_CREATED")
                        .languageCode("es").status("APPROVED").build()));
    }

    private WhatsAppConfig connectedConfig() {
        return WhatsAppConfig.builder()
                .organizationId(orgId).enabled(true).status(WhatsAppConnectionStatus.CONNECTED)
                .phoneNumberId("123456").accessToken("token").languageCode("es").build();
    }

    private void notifyOrderCreated(String phone, Boolean accepts) {
        useCase.notify(orgId, NotificationEvent.ORDER_CREATED, ClientNotificationUseCase.ENTITY_ORDER, orderId,
                phone, accepts, List.of("María", "ORD-000001", "Anillo", "12/09/2026"), "ORDER:" + orderId + ":CREATED");
    }

    private WhatsAppMessage savedMessage() {
        ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageGateway).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void enqueuesAndTriggersImmediateDispatchWhenEverythingApplies() {
        notifyOrderCreated("300 123 4567", true);

        WhatsAppMessage m = savedMessage();
        assertEquals(WhatsAppMessageStatus.PENDING, m.getStatus());
        assertEquals(orgId, m.getOrganizationId());
        assertEquals("573001234567", m.getToPhone());
        assertEquals("dp_pedido_creado_v1", m.getTemplateName());
        assertEquals("ORDER:" + orderId + ":CREATED", m.getIdempotencyKey());
        verify(trigger, times(1)).requestDispatch();
    }

    @Test
    void doesNothingWhenOrganizationHasNoWhatsAppModule() {
        when(configGateway.findByOrganizationId(orgId)).thenReturn(Optional.empty());

        notifyOrderCreated("3001234567", true);

        verify(messageGateway, never()).save(any());
        verify(trigger, never()).requestDispatch();
    }

    @Test
    void skipsWhenTemplateIsNotApprovedYet() {
        when(templateGateway.find(orgId, NotificationEvent.ORDER_CREATED.name(), "es")).thenReturn(Optional.empty());

        notifyOrderCreated("3001234567", true);

        WhatsAppMessage m = savedMessage();
        assertEquals(WhatsAppMessageStatus.SKIPPED, m.getStatus());
        assertEquals("TEMPLATE_NOT_APPROVED", m.getSkipReason());
        assertNull(m.getIdempotencyKey());
        verify(trigger, never()).requestDispatch();
    }

    @Test
    void skipsWhenClientDidNotAccept() {
        notifyOrderCreated("3001234567", false);

        assertEquals("CLIENT_NOT_ACCEPTED", savedMessage().getSkipReason());
        verify(trigger, never()).requestDispatch();
    }

    @Test
    void skipsInvalidPhones() {
        notifyOrderCreated("604 123 4567", true);

        assertEquals("INVALID_PHONE", savedMessage().getSkipReason());
    }

    @Test
    void doesNotSendTwiceTheSameEvent() {
        when(messageGateway.existsByIdempotencyKey("ORDER:" + orderId + ":CREATED")).thenReturn(true);

        notifyOrderCreated("3001234567", true);

        verify(messageGateway, never()).save(any());
    }

    @Test
    void respectsEventsDisabledByTheOrganization() {
        when(settingsGateway.findByOrganizationId(orgId)).thenReturn(Map.of("ORDER_CREATED", false));

        notifyOrderCreated("3001234567", true);

        verify(messageGateway, never()).save(any());
    }

    @Test
    void repairEventsOnlyApplyToJewelryOrganizations() {
        when(organizationGateway.findById(orgId)).thenReturn(Optional.of(
                Organization.builder().id(orgId).name("Lavandería").category("LAUNDRY").active(true).build()));

        useCase.notify(orgId, NotificationEvent.REPAIR_READY, ClientNotificationUseCase.ENTITY_REPAIR, orderId,
                "3001234567", true, List.of("María", "cadena", "$ 0"), "REPAIR:" + orderId + ":STATUS:READY_TO_DELIVER");

        verify(messageGateway, never()).save(any());
    }

    @Test
    void neverThrowsToTheCaller() {
        when(configGateway.findByOrganizationId(orgId)).thenThrow(new RuntimeException("BD caída"));

        notifyOrderCreated("3001234567", true); // no debe lanzar

        verify(messageGateway, never()).save(any());
    }
}
