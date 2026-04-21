package tech.bystep.planificador.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tech.bystep.planificador.model.gateways.WhatsAppGateway;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WhatsAppAdapter implements WhatsAppGateway {

    private static final String META_API_URL =
            "https://graph.facebook.com/v19.0/{phoneNumberId}/messages";

    @Value("${app.whatsapp.access-token:}")
    private String accessToken;

    @Value("${app.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    /**
     * When true: all events send the pre-approved "hello_world" template (sandbox testing).
     * When false: sends the real custom template by name.
     */
    @Value("${app.whatsapp.test-mode:false}")
    private boolean testMode;

    @Value("${app.whatsapp.language:es}")
    private String language;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendTemplate(String phoneNumber, String templateName, List<String> parameters) {
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            log.warn("WhatsApp not configured — skipping template '{}' to {}", templateName, phoneNumber);
            return;
        }
        String normalized = normalizePhone(phoneNumber);
        if (normalized == null) {
            log.warn("WhatsApp: invalid phone number '{}'", phoneNumber);
            return;
        }

        String resolvedTemplate = testMode ? "hello_world" : templateName;
        String resolvedLang    = testMode ? "en_US"       : language;
        List<String> resolvedParams = testMode ? List.of() : parameters;

        Map<String, Object> body = buildPayload(normalized, resolvedTemplate, resolvedLang, resolvedParams);

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
            log.info("WhatsApp template '{}' sent to {} (testMode={})", resolvedTemplate, normalized, testMode);
        } catch (Exception e) {
            log.warn("WhatsApp send failed to {} template '{}': {}", normalized, resolvedTemplate, e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(String to, String templateName,
                                             String langCode, List<String> params) {
        Map<String, Object> language = new LinkedHashMap<>();
        language.put("code", langCode);

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", language);

        if (!params.isEmpty()) {
            List<Map<String, String>> paramObjs = params.stream()
                    .map(p -> Map.of("type", "text", "text", p))
                    .collect(Collectors.toList());
            Map<String, Object> bodyComponent = new LinkedHashMap<>();
            bodyComponent.put("type", "body");
            bodyComponent.put("parameters", paramObjs);
            template.put("components", List.of(bodyComponent));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", "template");
        payload.put("template", template);
        return payload;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.isBlank()) return null;
        if (digits.length() == 10 && digits.startsWith("3")) {
            digits = "57" + digits;
        }
        return digits;
    }
}
