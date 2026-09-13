package tech.bystep.planificador.jpa.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tech.bystep.planificador.model.gateways.WhatsAppConfigGateway;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppConnectionStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WhatsAppConfigAdapter implements WhatsAppConfigGateway {

    private final WhatsAppConfigJpaRepository repository;
    private final CredentialCipher cipher;

    @Override
    public Optional<WhatsAppConfig> findByOrganizationId(UUID organizationId) {
        if (organizationId == null) return Optional.empty();
        return repository.findById(organizationId).map(this::toModel);
    }

    @Override
    public boolean isPhoneNumberIdUsedByAnotherOrganization(String phoneNumberId, UUID organizationId) {
        return repository.existsByPhoneNumberIdAndOrganizationIdNot(phoneNumberId, organizationId);
    }

    @Override
    public WhatsAppConfig save(WhatsAppConfig config) {
        WhatsAppConfigEntity existing = repository.findById(config.getOrganizationId()).orElse(null);
        String tokenEnc;
        if (config.getAccessToken() == null || config.getAccessToken().isBlank()) {
            tokenEnc = existing != null ? existing.getAccessTokenEnc() : null;
        } else if (existing != null && config.getAccessToken().equals(cipher.decrypt(existing.getAccessTokenEnc()))) {
            tokenEnc = existing.getAccessTokenEnc(); // mismo token: no re-cifrar
        } else {
            tokenEnc = cipher.encrypt(config.getAccessToken());
        }
        LocalDateTime now = LocalDateTime.now();
        WhatsAppConfigEntity entity = WhatsAppConfigEntity.builder()
                .organizationId(config.getOrganizationId())
                .enabled(config.isEnabled())
                .status((config.getStatus() != null ? config.getStatus() : WhatsAppConnectionStatus.PENDING).name())
                .wabaId(config.getWabaId())
                .phoneNumberId(config.getPhoneNumberId())
                .displayPhoneNumber(config.getDisplayPhoneNumber())
                .verifiedName(config.getVerifiedName())
                .accessTokenEnc(tokenEnc)
                .graphApiVersion(config.getGraphApiVersion())
                .languageCode(config.language())
                .supportContactText(config.getSupportContactText())
                .qualityRating(config.getQualityRating())
                .lastError(config.getLastError())
                .lastVerifiedAt(config.getLastVerifiedAt())
                .createdAt(existing != null && existing.getCreatedAt() != null ? existing.getCreatedAt()
                        : (config.getCreatedAt() != null ? config.getCreatedAt() : now))
                .updatedAt(now)
                .build();
        return toModel(repository.save(entity));
    }

    private WhatsAppConfig toModel(WhatsAppConfigEntity e) {
        WhatsAppConnectionStatus status;
        try {
            status = WhatsAppConnectionStatus.valueOf(e.getStatus());
        } catch (Exception ex) {
            status = WhatsAppConnectionStatus.PENDING;
        }
        String token = cipher.decrypt(e.getAccessTokenEnc());
        String lastError = e.getLastError();
        if (e.getAccessTokenEnc() != null && token == null) {
            // Hay token guardado pero no se pudo descifrar (llave ausente o distinta).
            status = WhatsAppConnectionStatus.ERROR;
            lastError = "No se pudo descifrar el token: revisa WHATSAPP_CREDENTIALS_KEY o vuelve a ingresar el token";
        }
        return WhatsAppConfig.builder()
                .organizationId(e.getOrganizationId())
                .enabled(e.isEnabled())
                .status(status)
                .wabaId(e.getWabaId())
                .phoneNumberId(e.getPhoneNumberId())
                .displayPhoneNumber(e.getDisplayPhoneNumber())
                .verifiedName(e.getVerifiedName())
                .accessToken(token)
                .graphApiVersion(e.getGraphApiVersion())
                .languageCode(e.getLanguageCode())
                .supportContactText(e.getSupportContactText())
                .qualityRating(e.getQualityRating())
                .lastError(lastError)
                .lastVerifiedAt(e.getLastVerifiedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
