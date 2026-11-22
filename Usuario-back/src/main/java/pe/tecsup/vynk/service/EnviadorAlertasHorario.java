package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.AlertaHorario;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.AlertaHorarioRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;

/** Envía a los celulares de los inscritos las alertas de cambio de horario registradas por el detector. */
@Service
public class EnviadorAlertasHorario {
    private static final Logger log = LoggerFactory.getLogger(EnviadorAlertasHorario.class);
    static final int MAX_INTENTOS = 3;
    private static final Duration ENVIO_ATASCADO = Duration.ofMinutes(5);

    private final AlertaHorarioRepository alertas;
    private final InscripcionRepository inscripciones;
    private final MongoTemplate mongo;
    private final NotificadorPush push;
    private final EntregaCelulares entrega;

    public EnviadorAlertasHorario(AlertaHorarioRepository alertas, InscripcionRepository inscripciones,
                                  DispositivoMovilRepository dispositivos, MongoTemplate mongo, NotificadorPush push) {
        this.alertas = alertas;
        this.inscripciones = inscripciones;
        this.mongo = mongo;
        this.push = push;
        this.entrega = new EntregaCelulares(dispositivos, push);
    }

    /** Devuelve cuántas alertas se entregaron. */
    public int enviarPendientes(Instant ahora) {
        if (!push.disponible()) return 0;
        mongo.updateMulti(new Query(Criteria.where("estado").is(AlertaHorario.ENVIANDO)
                        .and("reclamadaEn").lt(ahora.minus(ENVIO_ATASCADO))),
                new Update().set("estado", AlertaHorario.PENDIENTE).unset("reclamadaEn"), AlertaHorario.class);
        int enviadas = 0;
        for (AlertaHorario a : alertas.findTop200ByEstadoOrderByCreadaEnAsc(AlertaHorario.PENDIENTE)) {
            try {
                if (procesar(a, ahora)) enviadas++;
            } catch (RuntimeException e) {
                log.error("Falló la alerta de horario {}: {}", a.getId(), e.toString());
            }
        }
        return enviadas;
    }

    private boolean procesar(AlertaHorario pendiente, Instant ahora) {
        if (pendiente.getComienzaEn() == null || !pendiente.getComienzaEn().isAfter(ahora)) {
            cerrar(pendiente.getId(), AlertaHorario.PENDIENTE, AlertaHorario.VENCIDA, null, null);
            return false;
        }
        AlertaHorario a = mongo.findAndModify(
                new Query(Criteria.where("_id").is(pendiente.getId()).and("estado").is(AlertaHorario.PENDIENTE)),
                new Update().set("estado", AlertaHorario.ENVIANDO).set("reclamadaEn", ahora),
                FindAndModifyOptions.options().returnNew(true), AlertaHorario.class);
        if (a == null) return false; // otra ejecución la tomó
        try {
            Inscripcion i = inscripciones.findById(a.getInscripcionId()).orElse(null);
            Horario nuevo = new Horario(a.getFechaNueva(), a.getInicioNuevo(), a.getFinNuevo());
            if (i == null || !"inscrita".equalsIgnoreCase(i.getEstado())
                    || !nuevo.equals(new Horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin()))) {
                // Canceló la inscripción o el horario volvió a cambiar (hay otra alerta con el horario vigente).
                cerrar(a.getId(), AlertaHorario.ENVIANDO, AlertaHorario.CANCELADA, null, null);
                return false;
            }
            // Si el celular está apagado, FCM la guarda hasta que empiece la actividad (al menos una hora).
            Duration vida = Duration.between(ahora, a.getComienzaEn());
            if (vida.compareTo(Duration.ofHours(1)) < 0) vida = Duration.ofHours(1);
            switch (entrega.entregar(a.getUsuarioId(), MensajeCambioHorario.datos(a), vida)) {
                case ENTREGADA -> {
                    cerrar(a.getId(), AlertaHorario.ENVIANDO, AlertaHorario.ENVIADA, null, ahora);
                    return true;
                }
                // Sin celular registrado: se espera a que abra la app, sin gastar intentos.
                case SIN_CELULAR -> cerrar(a.getId(), AlertaHorario.ENVIANDO, AlertaHorario.PENDIENTE, null, null);
                case FALLO -> {
                    int intentos = a.getIntentos() + 1;
                    cerrar(a.getId(), AlertaHorario.ENVIANDO,
                            intentos >= MAX_INTENTOS ? AlertaHorario.FALLIDA : AlertaHorario.PENDIENTE, intentos, null);
                }
            }
            return false;
        } catch (RuntimeException e) {
            cerrar(a.getId(), AlertaHorario.ENVIANDO, AlertaHorario.PENDIENTE, a.getIntentos() + 1, null);
            throw e;
        }
    }

    private void cerrar(String id, String estadoActual, String nuevoEstado, Integer intentos, Instant enviadaEn) {
        Update cambio = new Update().set("estado", nuevoEstado).unset("reclamadaEn");
        if (intentos != null) cambio.set("intentos", intentos);
        if (enviadaEn != null) cambio.set("enviadaEn", enviadaEn);
        mongo.updateFirst(new Query(Criteria.where("_id").is(id).and("estado").is(estadoActual)), cambio, AlertaHorario.class);
    }
}
