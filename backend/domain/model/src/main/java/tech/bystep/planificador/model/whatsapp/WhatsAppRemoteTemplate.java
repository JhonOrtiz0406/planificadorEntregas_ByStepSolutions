package tech.bystep.planificador.model.whatsapp;

/** Plantilla tal como la reporta Meta para un WABA. */
public record WhatsAppRemoteTemplate(String id, String name, String language, String status, String rejectedReason) {
}
