package tech.bystep.planificador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.bystep.planificador.model.gateways.*;
import tech.bystep.planificador.usecase.*;

@Configuration
public class UseCasesConfig {

    @Bean
    public OrderUseCase orderUseCase(OrderGateway orderGateway, ReminderGateway reminderGateway,
                                     tech.bystep.planificador.model.gateways.WhatsAppGateway whatsAppGateway,
                                     NotificationGateway notificationGateway,
                                     tech.bystep.planificador.model.gateways.UserGateway userGateway) {
        return new OrderUseCase(orderGateway, reminderGateway, whatsAppGateway, notificationGateway, userGateway);
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
}
