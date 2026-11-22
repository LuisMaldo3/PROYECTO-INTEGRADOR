package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.RecordatorioActividadRepository;

/**
 * Programa un recordatorio por cada inscripción activa cuya actividad empieza pronto.
 * No toca el flujo de inscripción: revisa las inscripciones ya guardadas, así cubre también las
 * actividades que el organizador reprograma después y las inscripciones hechas desde la web.
 */
@Service
public class ProgramadorRecordatorios {
    private static final Logger log = LoggerFactory.getLogger(ProgramadorRecordatorios.class);
    /** Las fechas y horas de actividades y horarios se guardan en hora de Perú. */
    static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final InscripcionRepository inscripciones;
    private final RecordatorioActividadRepository recordatorios;
    private final Duration antelacion;
    private final Duration horizonte;

    public ProgramadorRecordatorios(InscripcionRepository inscripciones,
                                    RecordatorioActividadRepository recordatorios,
                                    @Value("${vynk.recordatorios.antelacion-minutos:30}") long antelacionMinutos,
                                    @Value("${vynk.recordatorios.horizonte-horas:48}") long horizonteHoras) {
        this.inscripciones = inscripciones;
        this.recordatorios = recordatorios;
        this.antelacion = Duration.ofMinutes(Math.max(1, antelacionMinutos));
        this.horizonte = Duration.ofHours(Math.max(1, horizonteHoras));
    }

    /** Crea o corrige los recordatorios de las próximas horas. Devuelve cuántos creó o reprogramó. */
    public int programar(Instant ahora) {
        Instant limite = ahora.plus(horizonte);
        int cambios = 0;
        for (Inscripcion i : inscripciones.findByEstadoAndFechaIn("inscrita", fechasEntre(ahora, limite))) {
            Instant inicio = inicioDe(i);
            if (inicio == null || !inicio.isAfter(ahora) || inicio.isAfter(limite)) continue;
            if (i.getId() == null || i.getEstudianteId() == null) continue;
            try {
                if (programarUna(i, inicio)) cambios++;
            } catch (DuplicateKeyException e) {
                // Otra ejecución lo creó al mismo tiempo: el índice único garantiza un solo aviso.
                log.debug("Recordatorio de la inscripción {} ya existe", i.getId());
            }
        }
        return cambios;
    }

    private boolean programarUna(Inscripcion i, Instant inicio) {
        Instant enviarEn = inicio.minus(antelacion);
        var existente = recordatorios.findByInscripcionId(i.getId());
        if (existente.isEmpty()) {
            RecordatorioActividad nuevo = new RecordatorioActividad();
            nuevo.setInscripcionId(i.getId());
            nuevo.setUsuarioId(i.getEstudianteId());
            nuevo.setActividadId(i.getActividadId());
            nuevo.setInicio(inicio);
            nuevo.setEnviarEn(enviarEn);
            recordatorios.save(nuevo);
            return true;
        }
        RecordatorioActividad r = existente.get();
        if (inicio.equals(r.getInicio()) && i.getEstudianteId().equals(r.getUsuarioId())) return false;
        // La actividad cambió de hora: se avisa de nuevo con la hora correcta.
        r.setUsuarioId(i.getEstudianteId());
        r.setActividadId(i.getActividadId());
        r.setInicio(inicio);
        r.setEnviarEn(enviarEn);
        r.setEstado(RecordatorioActividad.PENDIENTE);
        r.setIntentos(0);
        r.setReclamadoEn(null);
        r.setEnviadoEn(null);
        recordatorios.save(r);
        return true;
    }

    /** Momento exacto en que empieza la actividad de la inscripción, o null si la fecha u hora no sirven. */
    public static Instant inicioDe(Inscripcion i) {
        if (i == null || i.getFecha() == null || i.getHoraInicio() == null) return null;
        try {
            return LocalDateTime.of(LocalDate.parse(i.getFecha().strip()), LocalTime.parse(i.getHoraInicio().strip()))
                    .atZone(LIMA).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Fechas (en hora de Perú) que cubre el intervalo, de la de «desde» a la de «hasta», ambas incluidas. */
    static List<String> fechasEntre(Instant desde, Instant hasta) {
        List<String> fechas = new ArrayList<>();
        LocalDate dia = desde.atZone(LIMA).toLocalDate();
        LocalDate ultimo = hasta.atZone(LIMA).toLocalDate();
        for (; !dia.isAfter(ultimo); dia = dia.plusDays(1)) fechas.add(dia.toString());
        return fechas;
    }
}
