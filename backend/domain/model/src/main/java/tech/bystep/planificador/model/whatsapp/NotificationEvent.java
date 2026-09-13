package tech.bystep.planificador.model.whatsapp;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Catálogo de eventos que notifican al cliente final por WhatsApp.
 *
 * <p>Cada evento corresponde a UNA plantilla de Meta (categoría UTILITY, idioma "es").
 * Las plantillas son por WABA: al conectar una organización se crean en SU cuenta
 * de WhatsApp con "Sincronizar plantillas". Agregar un evento nuevo = agregar una
 * constante aquí + dispararlo desde el caso de uso. Nada más.</p>
 *
 * <p>Reglas de Meta que respeta el texto: sin promociones (si no, la reclasifican como
 * MARKETING), no empieza ni termina con una variable, y las variables van numeradas
 * {{1}}..{{n}} en orden.</p>
 */
public enum NotificationEvent {

    ORDER_CREATED("dp_pedido_creado_v1", "Pedido registrado", null,
            "Hola {{1}}, registramos tu pedido #{{2}}: {{3}}. Fecha estimada de entrega: {{4}}. "
                    + "Te avisaremos por este medio cualquier novedad.",
            List.of("María", "ORD-000001", "Anillo en oro", "12/09/2026")),

    ORDER_DATE_CHANGED("dp_pedido_cambio_fecha_v1", "Cambio de fecha de entrega", null,
            "Hola {{1}}, la fecha de entrega de tu pedido #{{2}} ({{3}}) fue actualizada. "
                    + "Nueva fecha: {{4}}. Gracias por tu paciencia.",
            List.of("María", "ORD-000001", "Anillo en oro", "15/09/2026")),

    ORDER_IN_PROGRESS("dp_pedido_en_proceso_v1", "Pedido en proceso", null,
            "Hola {{1}}, tu pedido #{{2}} ({{3}}) ya está en proceso. Te avisaremos cuando esté listo.",
            List.of("María", "ORD-000001", "Anillo en oro")),

    ORDER_READY("dp_pedido_listo_v1", "Pedido listo para entregar", null,
            "Hola {{1}}, tu pedido #{{2}} ({{3}}) está listo para entregar. "
                    + "Te contactaremos para coordinar la entrega.",
            List.of("María", "ORD-000001", "Anillo en oro")),

    ORDER_DELIVERED("dp_pedido_entregado_v1", "Pedido entregado", null,
            "Hola {{1}}, tu pedido #{{2}} ({{3}}) fue entregado. Gracias por confiar en nosotros.",
            List.of("María", "ORD-000001", "Anillo en oro")),

    ORDER_PAYMENT("dp_abono_pedido_v1", "Abono registrado a un pedido", null,
            "Hola {{1}}, registramos un abono de {{2}} a tu pedido #{{3}}. Saldo pendiente: {{4}}. "
                    + "Gracias por tu pago.",
            List.of("María", "$ 150.000", "ORD-000001", "$ 350.000")),

    REPAIR_RECEIVED("dp_arreglo_recibido_v1", "Arreglo recibido", Set.of("JEWELRY"),
            "Hola {{1}}, recibimos tu {{2}} para arreglo el {{3}}. Fecha estimada de entrega: {{4}}. "
                    + "Te avisaremos cuando esté listo.",
            List.of("María", "cadena en oro", "11/09/2026", "18/09/2026")),

    REPAIR_READY("dp_arreglo_listo_v1", "Arreglo listo", Set.of("JEWELRY"),
            "Hola {{1}}, tu {{2}} ya está listo. Puedes pasar a recogerlo. Saldo pendiente: {{3}}. Te esperamos.",
            List.of("María", "cadena en oro", "$ 40.000")),

    REPAIR_DELIVERED("dp_arreglo_entregado_v1", "Arreglo entregado", Set.of("JEWELRY"),
            "Hola {{1}}, entregamos tu {{2}}. Gracias por confiar en nosotros.",
            List.of("María", "cadena en oro")),

    REPAIR_PAYMENT("dp_abono_arreglo_v1", "Abono registrado a un arreglo", Set.of("JEWELRY"),
            "Hola {{1}}, registramos un abono de {{2}} al arreglo de tu {{3}}. Saldo pendiente: {{4}}. "
                    + "Gracias por tu pago.",
            List.of("María", "$ 20.000", "cadena en oro", "$ 40.000"));

    private final String templateName;
    private final String label;
    /** null = aplica a todas las categorías. */
    private final Set<String> categories;
    private final String bodyText;
    private final List<String> exampleParams;

    NotificationEvent(String templateName, String label, Set<String> categories,
                      String bodyText, List<String> exampleParams) {
        this.templateName = templateName;
        this.label = label;
        this.categories = categories;
        this.bodyText = bodyText;
        this.exampleParams = exampleParams;
    }

    public String templateName() { return templateName; }
    public String label() { return label; }
    public String bodyText() { return bodyText; }
    public List<String> exampleParams() { return exampleParams; }
    public int paramCount() { return exampleParams.size(); }

    public boolean appliesTo(String organizationCategory) {
        if (categories == null) return true;
        return organizationCategory != null && categories.contains(organizationCategory.toUpperCase());
    }

    /** Estado por defecto del evento si la organización no lo ha configurado. */
    public boolean defaultEnabled(String organizationCategory) {
        // Joyería maneja muchos estados intermedios (DESIGN, PRINT, POLISH...): "en proceso" no aplica.
        return !(this == ORDER_IN_PROGRESS && "JEWELRY".equalsIgnoreCase(organizationCategory));
    }

    public static List<NotificationEvent> applicableTo(String organizationCategory) {
        return Arrays.stream(values()).filter(e -> e.appliesTo(organizationCategory)).toList();
    }
}
