package tech.bystep.planificador.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtService {

    @Value("${app.security.jwt-secret}")
    private String jwtSecret;

    @Value("${app.security.jwt-expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "organizationId", user.getOrganizationId() != null ? user.getOrganizationId().toString() : "",
                "selectionToken", false
        );
        return Jwts.builder()
                .subject(user.getEmail())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateSelectionToken(User user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "role", "ORG_SELECTION",
                "organizationId", "",
                "selectionToken", true
        );
        return Jwts.builder()
                .subject(user.getEmail())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300_000L))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isSelectionToken(String token) {
        try {
            Object val = validateAndExtract(token).get("selectionToken");
            return Boolean.TRUE.equals(val);
        } catch (Exception e) {
            return false;
        }
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return validateAndExtract(token).getSubject();
    }

    public String extractRole(String token) {
        return validateAndExtract(token).get("role", String.class);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateAndExtract(token).get("userId", String.class));
    }

    public String extractOrganizationId(String token) {
        return validateAndExtract(token).get("organizationId", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = validateAndExtract(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
