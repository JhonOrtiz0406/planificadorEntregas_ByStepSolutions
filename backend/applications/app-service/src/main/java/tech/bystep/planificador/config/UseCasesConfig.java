package tech.bystep.planificador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.bystep.planificador.model.gateways.*;
import tech.bystep.planificador.usecase.*;

@Configuration
public class UseCasesConfig {

    @Bean
    public OrderUseCase orderUseCase(OrderGateway orderGateway, ReminderGateway reminderGateway) {
        return new OrderUseCase(orderGateway, reminderGateway);
    }

    @Bean
    public UserUseCase userUseCase(UserGateway userGateway) {
        return new UserUseCase(userGateway);
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
    public OrganizationUseCase organizationUseCase(OrganizationGateway organizationGateway) {
        return new OrganizationUseCase(organizationGateway);
    }
}
