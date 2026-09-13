package tech.bystep.planificador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.bystep.planificador.model.gateways.*;
import tech.bystep.planificador.usecase.*;

@Configuration
public class UseCasesConfig {

    @Bean
    public OrderUseCase orderUseCase(OrderGateway orderGateway, ReminderGateway reminderGateway,
                                     ClientNotificationUseCase clientNotificationUseCase,
                                     NotificationGateway notificationGateway,
                                     tech.bystep.planificador.model.gateways.UserGateway userGateway,
                                     tech.bystep.planificador.model.gateways.PaymentRecordGateway paymentRecordGateway,
                                     tech.bystep.planificador.model.gateways.StorageGateway storageGateway) {
        return new OrderUseCase(orderGateway, reminderGateway, clientNotificationUseCase, notificationGateway,
                userGateway, paymentRecordGateway, storageGateway);
    }

    @Bean
    public UserUseCase userUseCase(UserGateway userGateway,
                                   tech.bystep.planificador.model.gateways.UserOrgGateway userOrgGateway) {
        return new UserUseCase(userGateway, userOrgGateway);
    }

    @Bean
    public ReminderUseCase reminderUseCase(ReminderGateway reminderGateway, OrderGateway orderGateway,
                                           UserGateway userGateway, NotificationGateway notificationGateway) {
        return new ReminderUseCase(reminderGateway, orderGateway, userGateway, notificationGateway);
    }

    @Bean
    public InvitationUseCase invitationUseCase(InvitationGateway invitationGateway, EmailGateway emailGateway) {
        return new InvitationUseCase(invitationGateway, emailGateway);
    }

    @Bean
    public OrganizationUseCase organizationUseCase(OrganizationGateway organizationGateway,
                                                   tech.bystep.planificador.model.gateways.UserGateway userGateway,
                                                   tech.bystep.planificador.model.gateways.UserOrgGateway userOrgGateway,
                                                   tech.bystep.planificador.model.gateways.EmailGateway emailGateway) {
        return new OrganizationUseCase(organizationGateway, userGateway, userOrgGateway, emailGateway);
    }

    @Bean
    public CategoryStatusUseCase categoryStatusUseCase(
            tech.bystep.planificador.model.gateways.CategoryStatusGateway categoryStatusGateway) {
        return new CategoryStatusUseCase(categoryStatusGateway);
    }

    @Bean
    public RepairUseCase repairUseCase(RepairGateway repairGateway,
                                       tech.bystep.planificador.model.gateways.RepairPaymentGateway repairPaymentGateway,
                                       OrganizationGateway organizationGateway,
                                       tech.bystep.planificador.model.gateways.StorageGateway storageGateway,
                                       ClientNotificationUseCase clientNotificationUseCase) {
        return new RepairUseCase(repairGateway, repairPaymentGateway, organizationGateway, storageGateway,
                clientNotificationUseCase);
    }

    // ── WhatsApp por organización ───────────────────────────────────────────

    @Bean
    public ClientNotificationUseCase clientNotificationUseCase(OrganizationGateway organizationGateway,
                                                               WhatsAppConfigGateway whatsAppConfigGateway,
                                                               WhatsAppTemplateGateway whatsAppTemplateGateway,
                                                               NotificationSettingsGateway notificationSettingsGateway,
                                                               WhatsAppMessageGateway whatsAppMessageGateway,
                                                               WhatsAppDispatchTrigger whatsAppDispatchTrigger) {
        return new ClientNotificationUseCase(organizationGateway, whatsAppConfigGateway, whatsAppTemplateGateway,
                notificationSettingsGateway, whatsAppMessageGateway, whatsAppDispatchTrigger);
    }

    @Bean
    public WhatsAppDispatchUseCase whatsAppDispatchUseCase(WhatsAppMessageGateway whatsAppMessageGateway,
                                                           WhatsAppConfigGateway whatsAppConfigGateway,
                                                           WhatsAppCloudGateway whatsAppCloudGateway) {
        return new WhatsAppDispatchUseCase(whatsAppMessageGateway, whatsAppConfigGateway, whatsAppCloudGateway);
    }

    @Bean
    public WhatsAppAdminUseCase whatsAppAdminUseCase(OrganizationGateway organizationGateway,
                                                     WhatsAppConfigGateway whatsAppConfigGateway,
                                                     WhatsAppTemplateGateway whatsAppTemplateGateway,
                                                     NotificationSettingsGateway notificationSettingsGateway,
                                                     WhatsAppMessageGateway whatsAppMessageGateway,
                                                     WhatsAppCloudGateway whatsAppCloudGateway) {
        return new WhatsAppAdminUseCase(organizationGateway, whatsAppConfigGateway, whatsAppTemplateGateway,
                notificationSettingsGateway, whatsAppMessageGateway, whatsAppCloudGateway);
    }
}
