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

    private final String supportEmail;

    public EmailAdapter(JavaMailSender mailSender,
                        @Value("${app.url}") String appUrl,
                        @Value("${spring.mail.username:}") String fromAddress,
                        @Value("${app.support-email:${spring.mail.username:}}") String supportEmail) {
        this.mailSender = mailSender;
        this.appUrl = appUrl;
        this.fromAddress = fromAddress.trim().replaceAll("^\"|\"$", "");
        this.supportEmail = supportEmail.trim().replaceAll("^\"|\"$", "");
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
        sendHtml(to, subject, body);
    }

    @Override
    public void sendOrgClosedToAdmin(String to, String orgName, boolean deleted) {
        if (fromAddress.isBlank()) {
            log.warn("Skipping org-closed admin email to {} — MAIL_USERNAME not configured.", to);
            return;
        }
        String action = deleted ? "eliminada" : "inhabilitada";
        String subject = "Tu organización ha sido " + action + " – DeliveryPlanner";
        String body = buildOrgClosedAdminHtml(orgName, action, deleted);
        sendHtml(to, subject, body);
    }

    @Override
    public void sendOrgClosedToMember(String to, String orgName, String adminEmail, boolean deleted) {
        if (fromAddress.isBlank()) {
            log.warn("Skipping org-closed member email to {} — MAIL_USERNAME not configured.", to);
            return;
        }
        String action = deleted ? "eliminada" : "inhabilitada";
        String subject = "Tu acceso a " + orgName + " ha sido " + action + " – DeliveryPlanner";
        String body = buildOrgClosedMemberHtml(orgName, adminEmail, action);
        sendHtml(to, subject, body);
    }

    private void sendHtml(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, "DeliveryPlanner", "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    public void sendOrgReactivated(String to, String orgName) {
        if (fromAddress.isBlank()) {
            log.warn("Skipping org-reactivated email to {} — MAIL_USERNAME not configured.", to);
            return;
        }
        String subject = "Tu organización ha sido reactivada – DeliveryPlanner";
        String body = """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f0f2ff;font-family:'Inter','Segoe UI',Arial,sans-serif">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:16px;overflow:hidden;
                                    box-shadow:0 4px 24px rgba(79,70,229,.12)">
                        <tr>
                          <td style="background:linear-gradient(135deg,#16a34a,#15803d);
                                     padding:36px 40px;text-align:center">
                            <h1 style="margin:0;color:#fff;font-size:1.6rem;font-weight:700;letter-spacing:-.5px">
                              📦 DeliveryPlanner
                            </h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,.75);font-size:.9rem">
                              por ByStep Solutions
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px">
                            <div style="text-align:center;margin-bottom:24px">
                              <span style="font-size:3rem">✅</span>
                            </div>
                            <h2 style="margin:0 0 16px;color:#1e1b4b;font-size:1.25rem;font-weight:700;text-align:center">
                              ¡Organización reactivada!
                            </h2>
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              Te informamos que la organización <strong>%s</strong> ha sido
                              <strong>reactivada</strong> en la plataforma DeliveryPlanner por el
                              administrador de la plataforma.
                            </p>
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              Para restablecer el acceso de los miembros de tu organización,
                              deberás enviarles una nueva invitación desde el panel de administración
                              o contactar al soporte:
                              <a href="mailto:soporte@bystepsolutions.tech"
                                 style="color:#4f46e5;text-decoration:none">
                                soporte@bystepsolutions.tech
                              </a>
                            </p>
                            <hr style="border:none;border-top:1px solid #e5e7eb;margin:32px 0">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              Este mensaje es generado automáticamente por DeliveryPlanner.
                            </p>
                          </td>
                        </tr>
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
                """.formatted(orgName);
        sendHtml(to, subject, body);
    }

    @Override
    public void sendSupportContact(String userName, String userEmail, String phone,
                                   String organizationName, String message) {
        if (fromAddress.isBlank() || supportEmail.isBlank()) {
            log.warn("Skipping support contact email — mail not configured.");
            return;
        }
        String subject = "Solicitud de soporte – " + userName + " (" + userEmail + ")";
        String body = buildSupportContactHtml(userName, userEmail, phone, organizationName, message);
        sendHtml(supportEmail, subject, body);
    }

    private String buildSupportContactHtml(String userName, String userEmail, String phone,
                                           String organizationName, String message) {
        String org = organizationName != null && !organizationName.isBlank()
                ? esc(organizationName) : "N/A";
        String msg = message != null && !message.isBlank()
                ? esc(message) : "Sin mensaje adicional.";
        userName = esc(userName);
        userEmail = esc(userEmail);
        phone = esc(phone);
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
                        <tr>
                          <td style="background:linear-gradient(135deg,#4f46e5,#3730a3);
                                     padding:36px 40px;text-align:center">
                            <h1 style="margin:0;color:#fff;font-size:1.6rem;font-weight:700">
                              📦 DeliveryPlanner — Soporte
                            </h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,.75);font-size:.9rem">
                              Nueva solicitud de contacto
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px">
                            <h2 style="margin:0 0 24px;color:#1e1b4b;font-size:1.1rem;font-weight:700">
                              Datos del solicitante
                            </h2>
                            <table width="100%%" cellpadding="8" cellspacing="0"
                                   style="border-collapse:collapse;font-size:.95rem;color:#374151">
                              <tr style="border-bottom:1px solid #e5e7eb">
                                <td style="font-weight:600;width:140px">Nombre</td>
                                <td>%s</td>
                              </tr>
                              <tr style="border-bottom:1px solid #e5e7eb">
                                <td style="font-weight:600">Correo</td>
                                <td><a href="mailto:%s" style="color:#4f46e5">%s</a></td>
                              </tr>
                              <tr style="border-bottom:1px solid #e5e7eb">
                                <td style="font-weight:600">Celular</td>
                                <td>%s</td>
                              </tr>
                              <tr style="border-bottom:1px solid #e5e7eb">
                                <td style="font-weight:600">Organización</td>
                                <td>%s</td>
                              </tr>
                              <tr>
                                <td style="font-weight:600;vertical-align:top;padding-top:12px">Mensaje</td>
                                <td style="padding-top:12px;line-height:1.6">%s</td>
                              </tr>
                            </table>
                            <hr style="border:none;border-top:1px solid #e5e7eb;margin:32px 0">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              Generado automáticamente por DeliveryPlanner.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                     border-top:1px solid #e5e7eb">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              © 2025
                              <a href="https://www.bystepsolutions.tech/" style="color:#4f46e5;text-decoration:none">
                                ByStep Solutions S.A.S.
                              </a>
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(userName, userEmail, userEmail, phone, org, msg);
    }

    private String buildOrgClosedAdminHtml(String orgName, String action, boolean deleted) {
        String deletionWarning = deleted ? "" : """
                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="margin:24px 0;border-radius:10px;overflow:hidden;
                                          border:1px solid #fca5a5;background:#fff7f7">
                              <tr>
                                <td style="padding:16px 20px">
                                  <p style="margin:0;font-size:.9rem;color:#b91c1c;font-weight:600">
                                    ⚠️ Advertencia sobre la eliminación definitiva
                                  </p>
                                  <p style="margin:8px 0 0;font-size:.85rem;color:#7f1d1d;line-height:1.6">
                                    Si el administrador de la plataforma procede con la
                                    <strong>eliminación definitiva</strong> de esta organización,
                                    <strong>no será posible recuperarla</strong>. Todos los datos,
                                    pedidos e historial se perderán permanentemente y deberás
                                    crear una nueva organización desde cero, sin ninguna
                                    información previa.
                                  </p>
                                </td>
                              </tr>
                            </table>
                            """;
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
                        <tr>
                          <td style="background:linear-gradient(135deg,#dc2626,#991b1b);
                                     padding:36px 40px;text-align:center">
                            <h1 style="margin:0;color:#fff;font-size:1.6rem;font-weight:700;letter-spacing:-.5px">
                              📦 DeliveryPlanner
                            </h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,.75);font-size:.9rem">
                              por ByStep Solutions
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px">
                            <h2 style="margin:0 0 16px;color:#1e1b4b;font-size:1.25rem;font-weight:700">
                              Organización %s
                            </h2>
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              Te informamos que la organización <strong>%s</strong> ha sido
                              <strong>%s</strong> de la plataforma DeliveryPlanner por el administrador
                              de la plataforma. Tu acceso y el de todos los miembros ha sido revocado.
                            </p>
                            %s
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              Si consideras que esto es un error o necesitas más información,
                              por favor contacta a soporte:
                              <a href="mailto:soporte@bystepsolutions.tech"
                                 style="color:#4f46e5;text-decoration:none">
                                soporte@bystepsolutions.tech
                              </a>
                            </p>
                            <hr style="border:none;border-top:1px solid #e5e7eb;margin:32px 0">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              Este mensaje es generado automáticamente por DeliveryPlanner.
                            </p>
                          </td>
                        </tr>
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
                """.formatted(action, orgName, action, deletionWarning);
    }

    private String buildOrgClosedMemberHtml(String orgName, String adminEmail, String action) {
        String adminContact = adminEmail != null
                ? "Puedes contactar al administrador de tu organización: <a href=\"mailto:" + adminEmail + "\" style=\"color:#4f46e5;text-decoration:none\">" + adminEmail + "</a>, o bien,"
                : "También puedes";
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
                        <tr>
                          <td style="background:linear-gradient(135deg,#dc2626,#991b1b);
                                     padding:36px 40px;text-align:center">
                            <h1 style="margin:0;color:#fff;font-size:1.6rem;font-weight:700;letter-spacing:-.5px">
                              📦 DeliveryPlanner
                            </h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,.75);font-size:.9rem">
                              por ByStep Solutions
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:40px">
                            <h2 style="margin:0 0 16px;color:#1e1b4b;font-size:1.25rem;font-weight:700">
                              Tu acceso ha sido revocado
                            </h2>
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              Desde la organización <strong>%s</strong> en DeliveryPlanner, te informamos
                              que tu acceso ha sido <strong>%s</strong>. Tu cuenta ha sido desactivada.
                            </p>
                            <p style="margin:0 0 24px;color:#4b5563;line-height:1.6">
                              %s contacta a soporte para más información:
                              <a href="mailto:soporte@bystepsolutions.tech"
                                 style="color:#4f46e5;text-decoration:none">
                                soporte@bystepsolutions.tech
                              </a>
                            </p>
                            <hr style="border:none;border-top:1px solid #e5e7eb;margin:32px 0">
                            <p style="margin:0;color:#9ca3af;font-size:.8rem">
                              Este mensaje es generado automáticamente por DeliveryPlanner.
                            </p>
                          </td>
                        </tr>
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
                """.formatted(orgName, action, adminContact);
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

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
