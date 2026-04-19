package tech.bystep.planificador.email;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.gateways.EmailGateway;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class EmailAdapter implements EmailGateway {

    private final JavaMailSender mailSender;
    private final String appUrl;
    private final String fromAddress;

    public EmailAdapter(JavaMailSender mailSender,
                        @Value("${app.url}") String appUrl,
                        @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.appUrl = appUrl;
        // Sanitize: strip surrounding whitespace and accidental quotes
        this.fromAddress = fromAddress.trim().replaceAll("^\"|\"$", "");
    }

    @PostConstruct
    public void validate() {
        if (fromAddress.isBlank()) {
            log.warn("MAIL_USERNAME is not configured — invitation emails will NOT be sent.");
        } else {
            log.info("Email adapter ready. Sending from: {}", fromAddress);
        }
    }

    @Override
    public void sendInvitation(String to, String token, String organizationName) {
        if (fromAddress.isBlank()) {
            log.warn("Skipping invitation email to {} — MAIL_USERNAME is not configured.", to);
            return;
        }

        String invitationUrl = appUrl + "/auth/invite/" + token;
        String subject = "Invitación a " + organizationName + " – ByStep Solutions";
        String body = buildInvitationHtml(invitationUrl, organizationName);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, "DeliveryPlanner", "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Invitation email sent to {}", to);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send invitation email to {} (from: {}): {}", to, fromAddress, e.getMessage());
            // Do not throw — invitation record is already saved
        }
    }

    private String buildInvitationHtml(String invitationUrl, String organizationName) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f0f2ff;font-family:'Inter','Segoe UI',Arial,sans-serif">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:16px;overflow:hidden;
                                    box-shadow:0 4px 24px rgba(79,70,229,.12)">
                        <!-- Header -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#4f46e5,#3730a3);
                                     padding:36px 40px;text-align:center">
                            <h1 style="margin:0;color:#fff;font-size:1.6rem;font-weight:700;letter-spacing:-.5px">
                              📦 DeliveryPlanner
                            </h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,.75);font-size:.9rem">
                              por ByStep Solutions
                            </p>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding:40px">
                            <h2 style="margin:0 0 16px;color:#1e1b4b;font-size:1.25rem;font-weight:700">
                              ¡Tienes una invitación!
                            </h2>
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              Fuiste invitado a unirte a <strong>%s</strong>,
                              gestionada a través de DeliveryPlanner, la plataforma de planificación
                              de entregas de ByStep Solutions.
                              Haz clic en el botón para aceptar la invitación e iniciar sesión
                              con tu cuenta de Google.
                            </p>
                            <div style="text-align:center;margin:32px 0">
                              <a href="%s"
                                 style="display:inline-block;padding:14px 36px;
                                        background:linear-gradient(135deg,#4f46e5,#3730a3);
                                        color:#fff;font-size:1rem;font-weight:600;
                                        text-decoration:none;border-radius:10px;
                                        box-shadow:0 4px 14px rgba(79,70,229,.35)">
                                Aceptar Invitación
                              </a>
                            </div>
                            <p style="margin:24px 0 0;color:#9ca3af;font-size:.85rem;line-height:1.5">
                              Este enlace es válido por 7 días. Si no esperabas esta invitación,
                              puedes ignorar este correo de forma segura.
                            </p>
                            <hr style="border:none;border-top:1px solid #e5e7eb;margin:32px 0">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              También puedes copiar y pegar este enlace en tu navegador:<br>
                              <a href="%s" style="color:#4f46e5;word-break:break-all">%s</a>
                            </p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                     border-top:1px solid #e5e7eb">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              © 2025
                              <a href="https://www.bystepsolutions.tech/" style="color:#4f46e5;text-decoration:none">
                                ByStep Solutions S.A.S.
                              </a>
                              – Todos los derechos reservados
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(organizationName, invitationUrl, invitationUrl, invitationUrl);
    }
}
