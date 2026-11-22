package pe.tecsup.vynk.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada minuto programa los recordatorios que faltan y envía los que ya toca.
 * Se apaga con vynk.recordatorios.activo=false (por ejemplo, en una segunda copia del servidor).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "vynk.recordatorios.activo", havingValue = "true", matchIfMissing = true)
public class TareaRecordatorios {
    private static final Logger log = LoggerFactory.getLogger(TareaRecordatorios.class);
    private final ProgramadorRecordatorios programador;
    private final EnviadorRecordatorios enviador;

    public TareaRecordatorios(ProgramadorRecordatorios programador, EnviadorRecordatorios enviador) {
        this.programador = programador;
        this.enviador = enviador;
    }

    @Scheduled(fixedDelayString = "${vynk.recordatorios.intervalo-ms:60000}",
            initialDelayString = "${vynk.recordatorios.retraso-inicial-ms:20000}")
    public void ejecutar() {
        try {
            Instant ahora = Instant.now();
            int programados = programador.programar(ahora);
            int enviados = enviador.enviarPendientes(ahora);
            if (programados > 0 || enviados > 0)
                log.info("Recordatorios: {} programados o reprogramados, {} enviados", programados, enviados);
        } catch (RuntimeException e) {
            // Una falla de la base de datos no debe detener los ciclos siguientes.
            log.error("No se pudo completar el ciclo de recordatorios: {}", e.toString());
        }
    }
}
