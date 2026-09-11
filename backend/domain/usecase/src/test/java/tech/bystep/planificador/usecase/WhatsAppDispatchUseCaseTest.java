package tech.bystep.planificador.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.bystep.planificador.model.gateways.WhatsAppCloudGateway;
import tech.bystep.planificador.model.gateways.WhatsAppConfigGateway;
import tech.bystep.planificador.model.gateways.WhatsAppMessageGateway;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppConnectionStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessage;
import tech.bystep.planificador.model.whatsapp.WhatsAppMessageStatus;
import tech.bystep.planificador.model.whatsapp.WhatsAppSendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WhatsAppDispatchUseCaseTest {

    private final UUID orgA = UUID.randomUUID();
    private final UUID orgB = UUID.randomUUID();

    private WhatsAppMessageGateway messageGateway;
    private WhatsAppConfigGateway configGateway;
    private WhatsAppCloudGateway cloudGateway;
    private WhatsAppDispatchUseCase useCase;

    @BeforeEach
    void setUp() {
        messageGateway = mock(WhatsAppMessageGateway.class);
        configGateway = mock(WhatsAppConfigGateway.class);
        cloudGateway = mock(WhatsAppCloudGateway.class);
        useCase = new WhatsAppDispatchUseCase(messageGateway, configGateway, cloudGateway);
        when(configGateway.findByOrganizationId(orgA)).thenReturn(Optional.of(config(orgA, "111")));
        when(configGateway.findByOrganizationId(orgB)).thenReturn(Optional.of(config(orgB, "222")));
    }

    private WhatsAppConfig config(UUID orgId, String phoneNumberId) {
        return WhatsAppConfig.builder().organizationId(orgId).enabled(true)
                .status(WhatsAppConnectionStatus.CONNECTED).phoneNumberId(phoneNumberId)
                .accessToken("token-" + phoneNumberId).languageCode("es").build();
    }

    private WhatsAppMessage message(UUID orgId, int attempts) {
        return WhatsAppMessage.builder().id(UUID.randomUUID()).organizationId(orgId).eventKey("ORDER_READY")
                .toPhone("573001234567").templateName("dp_pedido_listo_v1").languageCode("es")
                .params(List.of("María", "ORD-000001", "Anillo")).status(WhatsAppMessageStatus.SENDING)
                .attempts(attempts).build();
    }

    private void queue(WhatsAppMessage... messages) {
        when(messageGateway.claimDue(any(LocalDateTime.class), anyInt())).thenReturn(List.of(messages));
    }

    private WhatsAppMessage lastSaved() {
        ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageGateway).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void eachMessageIsSentWithTheNumberOfItsOwnOrganization() {
        queue(message(orgA, 1), message(orgB, 1));
        when(cloudGateway.sendTemplate(any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.ok("wamid.x"));

        useCase.dispatchDue();

        verify(cloudGateway).sendTemplate(argThat(c -> c != null && "111".equals(c.getPhoneNumberId())),
                anyString(), anyString(), anyString(), anyList());
        verify(cloudGateway).sendTemplate(argThat(c -> c != null && "222".equals(c.getPhoneNumberId())),
                anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void successMarksMessageAsSent() {
        queue(message(orgA, 1));
        when(cloudGateway.sendTemplate(any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.ok("wamid.123"));

        useCase.dispatchDue();

        WhatsAppMessage saved = lastSaved();
        assertEquals(WhatsAppMessageStatus.SENT, saved.getStatus());
        assertEquals("wamid.123", saved.getWamid());
        assertNotNull(saved.getSentAt());
    }

    @Test
    void retryableErrorSchedulesAnotherAttempt() {
        queue(message(orgA, 1));
        when(cloudGateway.sendTemplate(any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.error("130429", "rate limit", true, false));

        useCase.dispatchDue();

        WhatsAppMessage saved = lastSaved();
        assertEquals(WhatsAppMessageStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getNextAttemptAt());
    }

    @Test
    void permanentErrorFailsTheMessage() {
        queue(message(orgA, 1));
        when(cloudGateway.sendTemplate(any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.error("131026", "undeliverable", false, false));

        useCase.dispatchDue();

        assertEquals(WhatsAppMessageStatus.FAILED, lastSaved().getStatus());
    }

    @Test
    void credentialErrorOnlyAffectsThatOrganization() {
        queue(message(orgA, 1));
        when(cloudGateway.sendTemplate(any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.error("190", "token inválido", false, true));

        useCase.dispatchDue();

        verify(configGateway).save(argThat(c -> orgA.equals(c.getOrganizationId())
                && c.getStatus() == WhatsAppConnectionStatus.ERROR));
        verify(configGateway, never()).save(argThat(c -> orgB.equals(c.getOrganizationId())));
        assertEquals(WhatsAppMessageStatus.PENDING, lastSaved().getStatus());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        queue(message(orgA, WhatsAppDispatchUseCase.MAX_ATTEMPTS));
        when(cloudGateway.sendTemplate(any(), anyString(), anyString(), anyString(), anyList()))
                .thenReturn(WhatsAppSendResult.error("131000", "error temporal", true, false));

        useCase.dispatchDue();

        assertEquals(WhatsAppMessageStatus.FAILED, lastSaved().getStatus());
    }

    @Test
    void disabledModuleSkipsWithoutCallingMeta() {
        when(configGateway.findByOrganizationId(orgA)).thenReturn(Optional.of(
                WhatsAppConfig.builder().organizationId(orgA).enabled(false)
                        .status(WhatsAppConnectionStatus.CONNECTED).build()));
        queue(message(orgA, 1));

        useCase.dispatchDue();

        verify(cloudGateway, never()).sendTemplate(any(), anyString(), anyString(), anyString(), anyList());
        assertEquals(WhatsAppMessageStatus.SKIPPED, lastSaved().getStatus());
    }

    @Test
    void backoffGrows() {
        assertEquals(1, WhatsAppDispatchUseCase.backoffMinutes(1));
        assertEquals(5, WhatsAppDispatchUseCase.backoffMinutes(2));
        assertEquals(30, WhatsAppDispatchUseCase.backoffMinutes(3));
    }
}
