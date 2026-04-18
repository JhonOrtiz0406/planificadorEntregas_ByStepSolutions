package tech.bystep.planificador.model.gateways;

public interface EmailGateway {

    /** Sends an invitation email with the acceptance link. */
    void sendInvitation(String to, String token);
}
