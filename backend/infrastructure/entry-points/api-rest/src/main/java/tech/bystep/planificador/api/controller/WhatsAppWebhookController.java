package tech.bystep.planificador.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    @Value("${app.whatsapp.webhook-verify-token:}")
    private String verifyToken;

    /**
     * Meta verification handshake — called once when registering the webhook URL.
     * Uses HttpServletRequest directly to avoid Spring MVC issues with dot-containing param names.
     */
    @GetMapping
    public ResponseEntity<String> verify(HttpServletRequest request) {
        String mode      = request.getParameter("hub.mode");
        String token     = request.getParameter("hub.verify_token");
        String challenge = request.getParameter("hub.challenge");

        log.info("Webhook verify — mode={}, receivedTokenLength={}, configuredTokenLength={}",
                mode, token != null ? token.length() : 0, verifyToken.length());

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Webhook failed — receivedToken='{}', configuredToken='{}'", token, verifyToken);
        return ResponseEntity.status(403).body("Forbidden");
    }

    /**
     * Receives message and status-change events from Meta.
     * Must respond 200 quickly; heavy processing should be async.
     */
    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        log.debug("WhatsApp webhook event received: {}", payload);
        // Future: parse incoming messages to open 24-hour reply window
        return ResponseEntity.ok().build();
    }
}
