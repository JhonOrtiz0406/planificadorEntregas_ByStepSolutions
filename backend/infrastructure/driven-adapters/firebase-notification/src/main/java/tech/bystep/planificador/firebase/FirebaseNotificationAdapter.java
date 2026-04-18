package tech.bystep.planificador.firebase;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.gateways.NotificationGateway;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseNotificationAdapter implements NotificationGateway {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .setToken(fcmToken)
                    .build();
            String response = firebaseMessaging.send(message);
            log.info("Firebase notification sent successfully: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending Firebase notification to token {}: {}", fcmToken, e.getMessage());
        }
    }

    @Override
    public void sendToMultipleTokens(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        if (fcmTokens.isEmpty()) return;
        try {
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .addAllTokens(fcmTokens)
                    .build();
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("Firebase multicast sent: {} success, {} failure",
                    response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Error sending Firebase multicast notification: {}", e.getMessage());
        }
    }

    @Override
    public void sendToTopic(String topic, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .setTopic(topic)
                    .build();
            String response = firebaseMessaging.send(message);
            log.info("Firebase topic notification sent: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending Firebase topic notification to {}: {}", topic, e.getMessage());
        }
    }
}
