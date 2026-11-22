package pe.tecsup.vynk.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada minuto: detecta los cambios de horario y de ubicación (y deja al día las inscripciones), programa los recordatorios que faltan
 * y envía al celular las alertas y recordatorios que ya tocan. Detectar primero evita avisar con una hora vieja.
 * Se apaga con vynk.recordatorios.activo=false (por ejemplo, en una segunda copia del servidor).
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "vynk.recordatorios.activo", havingValue = "true", matchIfMissing = true)
public class TareaRecordatorios {
    private static final Logger log = LoggerFactory.getLogger(TareaRecordatorios.class);
    private final DetectorCambiosHorario detector;
    private final EnviadorAlertasHorario alertas;
    private final DetectorCambiosUbicacion detectorUbicacion;
    private final EnviadorAlertasUbicacion alertasUbicacion;
    private final ProgramadorRecordatorios programador;
    private final EnviadorRecordatorios enviador;

    public TareaRecordatorios(DetectorCambiosHorario detector, EnviadorAlertasHorario alertas,
                              DetectorCambiosUbicacion detectorUbicacion, EnviadorAlertasUbicacion alertasUbicacion,
                              ProgramadorRecordatorios programador, EnviadorRecordatorios enviador) {
        this.detector = detector;
        this.alertas = alertas;
        this.detectorUbicacion = detectorUbicacion;
        this.alertasUbicacion = alertasUbicacion;
        this.programador = programador;
        this.enviador = enviador;
    }

    @Scheduled(fixedDelayString = "${vynk.recordatorios.intervalo-ms:60000}",
            initialDelayString = "${vynk.recordatorios.retraso-inicial-ms:20000}")
    public void ejecutar() {
        Instant ahora = Instant.now();
        try {
            int cambios = detector.detectar(ahora);
            int avisadas = alertas.enviarPendientes(ahora);
            if (cambios > 0 || avisadas > 0)
                log.info("Cambios de horario: {} alertas registradas, {} enviadas", cambios, avisadas);
        } catch (RuntimeException e) {
            // Una falla aquí no debe impedir los recordatorios ni los ciclos siguientes.
            log.error("No se pudo completar el ciclo de cambios de horario: {}", e.toString());
        }
        try {
            int cambios = detectorUbicacion.detectar(ahora);
            int avisadas = alertasUbicacion.enviarPendientes(ahora);
            if (cambios > 0 || avisadas > 0)
                log.info("Cambios de ubicación: {} alertas registradas, {} enviadas", cambios, avisadas);
        } catch (RuntimeException e) {
            log.error("No se pudo completar el ciclo de cambios de ubicación: {}", e.toString());
        }
        try {
            int programados = programador.programar(ahora);
            int enviados = enviador.enviarPendientes(ahora);
            if (programados > 0 || enviados > 0)
                log.info("Recordatorios: {} programados o reprogramados, {} enviados", programados, enviados);
        } catch (RuntimeException e) {
            log.error("No se pudo completar el ciclo de recordatorios: {}", e.toString());
        }
    }
}
