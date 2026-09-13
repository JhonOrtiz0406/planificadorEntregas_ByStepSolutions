package tech.bystep.planificador.model.whatsapp;

/** Error devuelto por la API de Meta en operaciones administrativas (plantillas, verificación). */
public class WhatsAppApiException extends RuntimeException {

    private final String code;

    public WhatsAppApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
