package tech.bystep.planificador.model.gateways;

public interface WhatsAppGateway {
    void sendMessage(String phoneNumber, String message);
}
