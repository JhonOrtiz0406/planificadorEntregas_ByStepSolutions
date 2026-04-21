package tech.bystep.planificador.model.gateways;

import java.util.List;

public interface WhatsAppGateway {
    void sendTemplate(String phoneNumber, String templateName, List<String> parameters);
}
