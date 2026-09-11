package tech.bystep.planificador.model.whatsapp;

public record WhatsAppPhoneInfo(boolean success, String displayPhoneNumber, String verifiedName,
                                String qualityRating, String errorMessage) {

    public static WhatsAppPhoneInfo error(String message) {
        return new WhatsAppPhoneInfo(false, null, null, null, message);
    }
}
