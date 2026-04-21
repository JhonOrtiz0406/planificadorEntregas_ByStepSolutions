package tech.bystep.planificador.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tech.bystep.planificador.model.gateways.WhatsAppGateway;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class WhatsAppAdapter implements WhatsAppGateway {

    private static final String META_API_URL =
            "https://graph.facebook.com/v19.0/{phoneNumberId}/messages";

    @Value("${app.whatsapp.access-token:}")
    private String accessToken;

    @Value("${app.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendMessage(String phoneNumber, String message) {
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            log.warn("WhatsApp not configured — skipping message to {}", phoneNumber);
            return;
        }
        String normalized = normalizePhone(phoneNumber);
        if (normalized == null) {
            log.warn("WhatsApp: invalid phone number '{}'", phoneNumber);
            return;
        }

        Map<String, Object> body = buildPayload(normalized, message);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        try {
            restTemplate.postForObject(
                    META_API_URL,
                    new HttpEntity<>(body, headers),
                    String.class,
                    phoneNumberId
            );
            log.info("WhatsApp sent to {}", normalized);
        } catch (Exception e) {
            log.warn("WhatsApp send failed to {}: {}", normalized, e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(String to, String messageText) {
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("preview_url", false);
        text.put("body", messageText);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", "text");
        payload.put("text", text);
        return payload;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.isBlank()) return null;
        // Auto-prefix Colombian country code for 10-digit numbers starting with 3
        if (digits.length() == 10 && digits.startsWith("3")) {
            digits = "57" + digits;
        }
        return digits;
    }
}
