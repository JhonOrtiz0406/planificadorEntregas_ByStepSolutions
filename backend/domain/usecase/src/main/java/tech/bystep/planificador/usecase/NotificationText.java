package tech.bystep.planificador.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Formatos y normalizaciones usados en las notificaciones al cliente final (Colombia). */
public final class NotificationText {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d/MM/yyyy");
    private static final int MAX_PARAM_LENGTH = 200;

    private NotificationText() {
    }

    /** $ 150.000 (sin decimales, separador de miles con punto). */
    public static String money(BigDecimal value) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
        if (v.signum() < 0) v = BigDecimal.ZERO;
        String digits = v.toPlainString();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            sb.append(digits.charAt(i));
            if (++count % 3 == 0 && i > 0) sb.append('.');
        }
        return "$ " + sb.reverse();
    }

    public static String date(LocalDate date) {
        return date == null ? "por confirmar" : date.format(DATE_FMT);
    }

    /**
     * Meta rechaza parámetros vacíos, con saltos de línea/tabs o con más de 4 espacios seguidos.
     */
    public static String param(String value) {
        if (value == null) return "-";
        String clean = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" {2,}", " ").trim();
        if (clean.isEmpty()) return "-";
        return clean.length() > MAX_PARAM_LENGTH ? clean.substring(0, MAX_PARAM_LENGTH) : clean;
    }

    /** Recorta textos largos (p. ej. la descripción de una joya) para que el mensaje se lea bien. */
    public static String shortText(String value, int maxLength) {
        String clean = param(value);
        if (clean.length() <= maxLength) return clean;
        return clean.substring(0, maxLength - 3).trim() + "...";
    }

    /**
     * Normaliza a formato internacional sin '+'. Celulares colombianos de 10 dígitos
     * (empiezan por 3) reciben el prefijo 57. Devuelve null si no parece un celular válido.
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.length() == 10 && digits.startsWith("3")) {
            return "57" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("573")) {
            return digits;
        }
        if (digits.startsWith("57")) {
            // Colombiano pero no es celular (fijo o incompleto): WhatsApp no aplica.
            return null;
        }
        // Números internacionales (clientes fuera de Colombia): E.164 de 11 a 15 dígitos.
        if (digits.length() >= 11 && digits.length() <= 15) {
            return digits;
        }
        return null;
    }

    /** 573001234567 → 57300****567 para logs y pantallas. */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 5) + "****" + phone.substring(phone.length() - 3);
    }
}
