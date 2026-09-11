package tech.bystep.planificador.model.gateways;

import tech.bystep.planificador.model.whatsapp.NotificationEvent;
import tech.bystep.planificador.model.whatsapp.WhatsAppConfig;
import tech.bystep.planificador.model.whatsapp.WhatsAppPhoneInfo;
import tech.bystep.planificador.model.whatsapp.WhatsAppRemoteTemplate;
import tech.bystep.planificador.model.whatsapp.WhatsAppSendResult;

import java.util.List;

/**
 * Meta WhatsApp Cloud API. Todas las operaciones reciben la configuración de la
 * organización: no existe un número ni un token global.
 */
public interface WhatsAppCloudGateway {

    WhatsAppSendResult sendTemplate(WhatsAppConfig config, String toPhone, String templateName,
                                    String languageCode, List<String> parameters);

    WhatsAppPhoneInfo getPhoneInfo(WhatsAppConfig config);

    /**
     * Crea la plantilla en el WABA de la organización y devuelve lo que reporta Meta.
     *
     * @throws tech.bystep.planificador.model.whatsapp.WhatsAppApiException si Meta la rechaza
     */
    WhatsAppRemoteTemplate createTemplate(WhatsAppConfig config, NotificationEvent event, String footerText);

    List<WhatsAppRemoteTemplate> listTemplates(WhatsAppConfig config);
}
