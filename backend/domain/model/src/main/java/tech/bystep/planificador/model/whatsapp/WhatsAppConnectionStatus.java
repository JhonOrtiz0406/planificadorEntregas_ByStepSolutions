package tech.bystep.planificador.model.whatsapp;

public enum WhatsAppConnectionStatus {
    /** Credenciales guardadas pero aún no verificadas contra Meta. */
    PENDING,
    /** Verificado: el número responde en la Cloud API con ese token. */
    CONNECTED,
    /** Token inválido, número no registrado, permisos insuficientes, etc. */
    ERROR
}
