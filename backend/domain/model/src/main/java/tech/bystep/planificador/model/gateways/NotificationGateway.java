package tech.bystep.planificador.model.gateways;

import java.util.List;
import java.util.Map;

public interface NotificationGateway {

    void sendToToken(String fcmToken, String title, String body, Map<String, String> data);

    void sendToMultipleTokens(List<String> fcmTokens, String title, String body, Map<String, String> data);

    void sendToTopic(String topic, String title, String body, Map<String, String> data);
}
