package tech.bystep.planificador.jpa.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra los tokens de Meta de cada organización con AES-256-GCM.
 * Llave: variable de entorno WHATSAPP_CREDENTIALS_KEY (32 bytes en base64,
 * generar con {@code openssl rand -base64 32}).
 */
@Component
public class CredentialCipher {

    private static final String PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public CredentialCipher(@Value("${app.whatsapp.credentials-key:}") String base64Key) {
        SecretKeySpec parsed = null;
        if (base64Key != null && !base64Key.isBlank()) {
            byte[] raw = Base64.getDecoder().decode(base64Key.trim());
            if (raw.length != 32) {
                throw new IllegalStateException("WHATSAPP_CREDENTIALS_KEY debe ser de 32 bytes (openssl rand -base64 32)");
            }
            parsed = new SecretKeySpec(raw, "AES");
        }
        this.key = parsed;
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return null;
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar la credencial", e);
        }
    }

    /** Devuelve null si no hay valor o si no se puede descifrar (llave cambiada, dato corrupto). */
    public String decrypt(String stored) {
        if (stored == null || stored.isBlank() || key == null || !stored.startsWith(PREFIX)) return null;
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException(
                    "Falta configurar WHATSAPP_CREDENTIALS_KEY en el servidor para guardar credenciales de WhatsApp");
        }
    }
}
