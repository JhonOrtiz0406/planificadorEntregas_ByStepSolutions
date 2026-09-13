package tech.bystep.planificador.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tech.bystep.planificador.model.gateways.WhatsAppCloudGateway;
import tech.bystep.planificador.model.whatsapp.NotificationEvent;
import tech.bystep.planificador.model.whatsapp.WhatsAppApiException;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppPhoneInfo;
import tech.bystep.planificador.model.whatsapp.WhatsAppRemoteTemplate;
import tech.bystep.planificador.model.whatsapp.WhatsAppSendResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cliente de la Meta WhatsApp Cloud API. No tiene número ni token propios: todo sale
 * de la {@link WhatsAppConfig} de la organización que envía.
 */
@Slf4j
@Component
public class MetaWhatsAppCloudAdapter implements WhatsAppCloudGateway {

    private static final String GRAPH_BASE_URL = "https://graph.facebook.com";

    /** Errores temporales de Meta: se reintenta más tarde. */
    private static final Set<String> RETRYABLE_CODES = Set.of(
            "1", "2", "4", "17", "80007", "130429", "131000", "131016", "131048", "131056", "133004");

    /** Errores de token/permisos: la configuración de ESA organización está mal. */
    private static final Set<String> CREDENTIAL_CODES = Set.of("190", "10", "3", "200", "294");

    private static final Map<String, String> FRIENDLY = Map.ofEntries(
            Map.entry("190", "El token de acceso de Meta es inválido o fue revocado"),
            Map.entry("10", "El token no tiene permisos sobre este número (whatsapp_business_messaging)"),
            Map.entry("200", "El token no tiene permisos suficientes"),
            Map.entry("100", "Parámetro inválido (revisa el Phone Number ID / WABA ID)"),
            Map.entry("131026", "El número del cliente no tiene WhatsApp o no puede recibir el mensaje"),
            Map.entry("131042", "La cuenta de WhatsApp no tiene un método de pago válido en Meta"),
            Map.entry("131049", "Meta no entregó el mensaje para cuidar la experiencia del usuario"),
            Map.entry("131050", "El cliente dejó de aceptar mensajes de este negocio"),
            Map.entry("131047", "Pasaron más de 24 horas desde la última respuesta del cliente"),
            Map.entry("131051", "Tipo de mensaje no soportado"),
            Map.entry("131064", "Límite de mensajes alcanzado por clasificación de plantillas"),
            Map.entry("132000", "La cantidad de parámetros no coincide con la plantilla"),
            Map.entry("132001", "La plantilla no existe o no está aprobada en ese idioma"),
            Map.entry("132005", "El texto de la plantilla con parámetros es demasiado largo"),
            Map.entry("132007", "La plantilla viola las políticas de Meta"),
            Map.entry("132012", "Los parámetros no tienen el formato esperado"),
            Map.entry("133010", "El número no está registrado en la Cloud API"),
            Map.entry("130429", "Se superó la velocidad de envío del número"),
            Map.entry("131048", "Meta limitó temporalmente los envíos de este número"));

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String defaultVersion;
    private final boolean globalEnabled;
    private final boolean dryRun;

    public MetaWhatsAppCloudAdapter(@Value("${app.whatsapp.graph-version:v23.0}") String defaultVersion,
                                    @Value("${app.whatsapp.enabled:true}") boolean globalEnabled,
                                    @Value("${app.whatsapp.dry-run:false}") boolean dryRun) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(15_000);
        this.restClient = RestClient.builder()
                .baseUrl(GRAPH_BASE_URL)
                .requestFactory(requestFactory)
                .build();
        this.defaultVersion = defaultVersion;
        this.globalEnabled = globalEnabled;
        this.dryRun = dryRun;
        if (dryRun) {
            log.warn("WhatsApp en modo DRY-RUN: no se envía nada a Meta");
        }
    }

    // ── Envío ────────────────────────────────────────────────────────────────

    @Override
    public WhatsAppSendResult sendTemplate(WhatsAppConfig config, String toPhone, String templateName,
                                           String languageCode, List<String> parameters) {
        if (!globalEnabled) {
            return WhatsAppSendResult.error("GLOBAL_DISABLED", "WhatsApp está deshabilitado en el servidor", false, false);
        }
        if (dryRun) {
            log.info("[DRY-RUN] WhatsApp org={} plantilla={} a {}", config.getOrganizationId(), templateName, mask(toPhone));
            return WhatsAppSendResult.ok("dryrun." + UUID.randomUUID());
        }
        Map<String, Object> payload = templateMessagePayload(toPhone, templateName, languageCode, parameters);
        try {
            String response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", version(config), config.getPhoneNumberId())
                    .headers(h -> h.setBearerAuth(config.getAccessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            JsonNode node = mapper.readTree(response == null ? "{}" : response);
            String wamid = node.path("messages").path(0).path("id").asText(null);
            log.info("WhatsApp enviado org={} plantilla={} a {}", config.getOrganizationId(), templateName, mask(toPhone));
            return WhatsAppSendResult.ok(wamid);
        } catch (RestClientResponseException e) {
            WhatsAppSendResult result = mapError(e.getStatusCode().value(), e.getResponseBodyAsString());
            log.warn("WhatsApp falló org={} plantilla={} a {}: [{}] {}", config.getOrganizationId(), templateName,
                    mask(toPhone), result.errorCode(), result.errorMessage());
            return result;
        } catch (ResourceAccessException e) {
            return WhatsAppSendResult.error("NETWORK", "Sin conexión con Meta: " + e.getMessage(), true, false);
        } catch (Exception e) {
            return WhatsAppSendResult.error("INTERNAL", e.getMessage(), true, false);
        }
    }

    // ── Verificación del número ─────────────────────────────────────────────

    @Override
    public WhatsAppPhoneInfo getPhoneInfo(WhatsAppConfig config) {
        if (dryRun) {
            return new WhatsAppPhoneInfo(true, "+57 300 000 0000", "DRY-RUN", "GREEN", null);
        }
        try {
            String response = restClient.get()
                    .uri("/{version}/{phoneNumberId}?fields=display_phone_number,verified_name,quality_rating",
                            version(config), config.getPhoneNumberId())
                    .headers(h -> h.setBearerAuth(config.getAccessToken()))
                    .retrieve()
                    .body(String.class);
            JsonNode node = mapper.readTree(response == null ? "{}" : response);
            return new WhatsAppPhoneInfo(true,
                    node.path("display_phone_number").asText(null),
                    node.path("verified_name").asText(null),
                    node.path("quality_rating").asText(null),
                    null);
        } catch (RestClientResponseException e) {
            WhatsAppSendResult error = mapError(e.getStatusCode().value(), e.getResponseBodyAsString());
            return WhatsAppPhoneInfo.error("[" + error.errorCode() + "] " + error.errorMessage());
        } catch (Exception e) {
            return WhatsAppPhoneInfo.error("No se pudo contactar a Meta: " + e.getMessage());
        }
    }

    // ── Plantillas ───────────────────────────────────────────────────────────

    @Override
    public WhatsAppRemoteTemplate createTemplate(WhatsAppConfig config, NotificationEvent event, String footerText) {
        if (dryRun) {
            return new WhatsAppRemoteTemplate("dryrun", event.templateName(), config.language(), "APPROVED", null);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "BODY");
        body.put("text", event.bodyText());
        body.put("example", Map.of("body_text", List.of(event.exampleParams())));

        List<Map<String, Object>> components = new ArrayList<>();
        components.add(body);
        if (footerText != null && !footerText.isBlank()) {
            components.add(Map.of("type", "FOOTER", "text", footerText.trim()));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", event.templateName());
        payload.put("language", config.language());
        payload.put("category", "UTILITY");
        payload.put("components", components);

        try {
            String response = restClient.post()
                    .uri("/{version}/{wabaId}/message_templates", version(config), config.getWabaId())
                    .headers(h -> h.setBearerAuth(config.getAccessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            JsonNode node = mapper.readTree(response == null ? "{}" : response);
            return new WhatsAppRemoteTemplate(node.path("id").asText(null), event.templateName(), config.language(),
                    node.path("status").asText("PENDING"), null);
        } catch (RestClientResponseException e) {
            WhatsAppSendResult error = mapError(e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new WhatsAppApiException(error.errorCode(), error.errorMessage());
        } catch (WhatsAppApiException e) {
            throw e;
        } catch (Exception e) {
            throw new WhatsAppApiException("INTERNAL", "No se pudo crear la plantilla: " + e.getMessage());
        }
    }

    @Override
    public List<WhatsAppRemoteTemplate> listTemplates(WhatsAppConfig config) {
        if (dryRun) {
            return java.util.Arrays.stream(NotificationEvent.values())
                    .map(e -> new WhatsAppRemoteTemplate("dryrun", e.templateName(), config.language(), "APPROVED", null))
                    .toList();
        }
        List<WhatsAppRemoteTemplate> result = new ArrayList<>();
        try {
            String response = restClient.get()
                    .uri("/{version}/{wabaId}/message_templates?fields=id,name,language,status,rejected_reason&limit=100",
                            version(config), config.getWabaId())
                    .headers(h -> h.setBearerAuth(config.getAccessToken()))
                    .retrieve()
                    .body(String.class);
            for (int page = 0; page < 10 && response != null; page++) {
                JsonNode node = mapper.readTree(response);
                for (JsonNode t : node.path("data")) {
                    result.add(new WhatsAppRemoteTemplate(
                            t.path("id").asText(null),
                            t.path("name").asText(null),
                            t.path("language").asText(null),
                            t.path("status").asText(null),
                            t.path("rejected_reason").asText(null)));
                }
                String next = node.path("paging").path("next").asText(null);
                if (next == null || next.isBlank()) break;
                response = restClient.get()
                        .uri(URI.create(next))
                        .headers(h -> h.setBearerAuth(config.getAccessToken()))
                        .retrieve()
                        .body(String.class);
            }
            return result;
        } catch (RestClientResponseException e) {
            WhatsAppSendResult error = mapError(e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new WhatsAppApiException(error.errorCode(), error.errorMessage());
        } catch (Exception e) {
            throw new WhatsAppApiException("INTERNAL", "No se pudieron consultar las plantillas: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static Map<String, Object> templateMessagePayload(String to, String templateName, String languageCode,
                                                      List<String> params) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", languageCode == null || languageCode.isBlank() ? "es" : languageCode));
        if (params != null && !params.isEmpty()) {
            List<Map<String, String>> parameters = params.stream()
                    .map(p -> Map.of("type", "text", "text", p))
                    .toList();
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", "template");
        payload.put("template", template);
        return payload;
    }

    WhatsAppSendResult mapError(int httpStatus, String body) {
        String code = String.valueOf(httpStatus);
        String message = "Error HTTP " + httpStatus + " de Meta";
        try {
            JsonNode error = mapper.readTree(body == null || body.isBlank() ? "{}" : body).path("error");
            if (!error.isMissingNode()) {
                if (error.hasNonNull("code")) code = error.get("code").asText();
                String metaMessage = error.path("message").asText("");
                String details = error.path("error_data").path("details").asText("");
                message = FRIENDLY.getOrDefault(code, metaMessage)
                        + (details.isBlank() || details.equals(metaMessage) ? "" : " — " + details);
            }
        } catch (Exception ignored) {
            // cuerpo no JSON: se usa el HTTP status
        }
        boolean credentials = CREDENTIAL_CODES.contains(code) || httpStatus == 401;
        boolean retryable = !credentials && (httpStatus >= 500 || httpStatus == 429 || RETRYABLE_CODES.contains(code));
        return WhatsAppSendResult.error(code, message, retryable, credentials);
    }

    private String version(WhatsAppConfig config) {
        String v = config.getGraphApiVersion();
        if (v != null && v.matches("v\\d+\\.\\d+")) return v;
        return defaultVersion;
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 5) + "****" + phone.substring(phone.length() - 3);
    }
}
