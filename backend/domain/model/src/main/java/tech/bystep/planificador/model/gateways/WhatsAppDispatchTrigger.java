package tech.bystep.planificador.model.gateways;

/** Pide procesar la cola de WhatsApp ya (envío inmediato, sin esperar al barrido periódico). */
public interface WhatsAppDispatchTrigger {
    void requestDispatch();
}
