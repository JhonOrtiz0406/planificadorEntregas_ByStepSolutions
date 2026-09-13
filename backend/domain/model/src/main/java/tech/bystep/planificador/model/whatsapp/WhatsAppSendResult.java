package tech.bystep.planificador.model.whatsapp;

/**
 * Resultado de una llamada a Meta.
 *
 * @param success           true si Meta aceptó el mensaje
 * @param wamid             id del mensaje en WhatsApp (solo si success)
 * @param errorCode         código de error de Meta o interno
 * @param errorMessage      detalle legible
 * @param retryable         vale la pena reintentar más tarde
 * @param credentialsError  el token/permisos de ESA organización están mal
 */
public record WhatsAppSendResult(boolean success, String wamid, String errorCode, String errorMessage,
                                 boolean retryable, boolean credentialsError) {

    public static WhatsAppSendResult ok(String wamid) {
        return new WhatsAppSendResult(true, wamid, null, null, false, false);
    }

    public static WhatsAppSendResult error(String code, String message, boolean retryable, boolean credentialsError) {
        return new WhatsAppSendResult(false, null, code, message, retryable, credentialsError);
    }
}
