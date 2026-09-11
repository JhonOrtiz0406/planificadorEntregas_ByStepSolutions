package tech.bystep.planificador.model.whatsapp;

public enum WhatsAppMessageStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    /** No se envió por una regla de negocio (ver skipReason). */
    SKIPPED
}
