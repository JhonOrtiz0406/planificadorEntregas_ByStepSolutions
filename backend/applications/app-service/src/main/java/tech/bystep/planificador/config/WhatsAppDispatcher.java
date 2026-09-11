package tech.bystep.planificador.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.bystep.planificador.model.gateways.WhatsAppDispatchTrigger;
import tech.bystep.planificador.usecase.WhatsAppDispatchUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Procesa la cola de WhatsApp en segundo plano.
 * <ul>
 *   <li>Envío inmediato: cada notificación nueva dispara un procesamiento al instante.</li>
 *   <li>Barrido periódico: reintentos y cualquier mensaje que haya quedado pendiente.</li>
 * </ul>
 * Las solicitudes se agrupan: nunca hay dos procesamientos simultáneos en esta instancia
 * (y entre instancias, la BD reparte los mensajes con FOR UPDATE SKIP LOCKED).
 */
@Slf4j
@Component
public class WhatsAppDispatcher implements WhatsAppDispatchTrigger {

    private final WhatsAppDispatchUseCase dispatchUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "whatsapp-dispatcher");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean rerunRequested = new AtomicBoolean(false);

    public WhatsAppDispatcher(WhatsAppDispatchUseCase dispatchUseCase) {
        this.dispatchUseCase = dispatchUseCase;
    }

    @Override
    public void requestDispatch() {
        rerunRequested.set(true);
        if (running.compareAndSet(false, true)) {
            try {
                executor.submit(this::runLoop);
            } catch (Exception e) {
                running.set(false);
                log.warn("No se pudo programar el envío de WhatsApp: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.whatsapp.sweep-interval-ms:15000}", initialDelay = 20000)
    public void sweep() {
        requestDispatch();
    }

    private void runLoop() {
        try {
            while (rerunRequested.getAndSet(false)) {
                try {
                    int processed = dispatchUseCase.dispatchDue();
                    if (processed > 0) {
                        log.info("WhatsApp: {} mensaje(s) procesado(s)", processed);
                    }
                } catch (Exception e) {
                    log.error("WhatsApp: error procesando la cola: {}", e.getMessage(), e);
                }
            }
        } finally {
            running.set(false);
            // Si llegó una solicitud justo al terminar, no la perdemos.
            if (rerunRequested.get() && running.compareAndSet(false, true)) {
                executor.submit(this::runLoop);
            }
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
