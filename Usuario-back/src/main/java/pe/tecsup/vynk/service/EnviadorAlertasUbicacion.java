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
import pe.tecsup.vynk.model.AlertaUbicacion;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.AlertaUbicacionRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;

/** Envía a los celulares de los inscritos las alertas de cambio de ubicación registradas por el detector. */
@Service
public class EnviadorAlertasUbicacion {
    private static final Logger log = LoggerFactory.getLogger(EnviadorAlertasUbicacion.class);
    static final int MAX_INTENTOS = 3;
    private static final Duration ENVIO_ATASCADO = Duration.ofMinutes(5);
    /** Actividad sin fecha: FCM guarda la alerta una semana si el celular está apagado. */
    private static final Duration VIDA_SIN_FECHA = Duration.ofDays(7);

    private final AlertaUbicacionRepository alertas;
    private final InscripcionRepository inscripciones;
    private final MongoTemplate mongo;
    private final NotificadorPush push;
    private final EntregaCelulares entrega;

    public EnviadorAlertasUbicacion(AlertaUbicacionRepository alertas, InscripcionRepository inscripciones,
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
        mongo.updateMulti(new Query(Criteria.where("estado").is(AlertaUbicacion.ENVIANDO)
                        .and("reclamadaEn").lt(ahora.minus(ENVIO_ATASCADO))),
                new Update().set("estado", AlertaUbicacion.PENDIENTE).unset("reclamadaEn"), AlertaUbicacion.class);
        int enviadas = 0;
        for (AlertaUbicacion a : alertas.findTop200ByEstadoOrderByCreadaEnAsc(AlertaUbicacion.PENDIENTE)) {
            try {
                if (procesar(a, ahora)) enviadas++;
            } catch (RuntimeException e) {
                log.error("Falló la alerta de ubicación {}: {}", a.getId(), e.toString());
            }
        }
        return enviadas;
    }

    private boolean procesar(AlertaUbicacion pendiente, Instant ahora) {
        if (pendiente.getComienzaEn() != null && !pendiente.getComienzaEn().isAfter(ahora)) {
            cerrar(pendiente.getId(), AlertaUbicacion.PENDIENTE, AlertaUbicacion.VENCIDA, null, null);
            return false;
        }
        AlertaUbicacion a = mongo.findAndModify(
                new Query(Criteria.where("_id").is(pendiente.getId()).and("estado").is(AlertaUbicacion.PENDIENTE)),
                new Update().set("estado", AlertaUbicacion.ENVIANDO).set("reclamadaEn", ahora),
                FindAndModifyOptions.options().returnNew(true), AlertaUbicacion.class);
        if (a == null) return false; // otra ejecución la tomó
        try {
            Inscripcion i = inscripciones.findById(a.getInscripcionId()).orElse(null);
            if (i == null || !"inscrita".equalsIgnoreCase(i.getEstado())
                    || !DetectorCambiosUbicacion.limpio(a.getUbicacionNuevaId()).equals(DetectorCambiosUbicacion.limpio(i.getUbicacion()))) {
                // Canceló la inscripción o el lugar volvió a cambiar (hay otra alerta con el lugar vigente).
                cerrar(a.getId(), AlertaUbicacion.ENVIANDO, AlertaUbicacion.CANCELADA, null, null);
                return false;
            }
            Duration vida = a.getComienzaEn() == null ? VIDA_SIN_FECHA : Duration.between(ahora, a.getComienzaEn());
            if (vida.compareTo(Duration.ofHours(1)) < 0) vida = Duration.ofHours(1);
            switch (entrega.entregar(a.getUsuarioId(), MensajeCambioUbicacion.datos(a), vida)) {
                case ENTREGADA -> {
                    cerrar(a.getId(), AlertaUbicacion.ENVIANDO, AlertaUbicacion.ENVIADA, null, ahora);
                    return true;
                }
                // Sin celular registrado: se espera a que abra la app, sin gastar intentos.
                case SIN_CELULAR -> cerrar(a.getId(), AlertaUbicacion.ENVIANDO, AlertaUbicacion.PENDIENTE, null, null);
                case FALLO -> {
                    int intentos = a.getIntentos() + 1;
                    cerrar(a.getId(), AlertaUbicacion.ENVIANDO,
                            intentos >= MAX_INTENTOS ? AlertaUbicacion.FALLIDA : AlertaUbicacion.PENDIENTE, intentos, null);
                }
            }
            return false;
        } catch (RuntimeException e) {
            cerrar(a.getId(), AlertaUbicacion.ENVIANDO, AlertaUbicacion.PENDIENTE, a.getIntentos() + 1, null);
            throw e;
        }
    }

    private void cerrar(String id, String estadoActual, String nuevoEstado, Integer intentos, Instant enviadaEn) {
        Update cambio = new Update().set("estado", nuevoEstado).unset("reclamadaEn");
        if (intentos != null) cambio.set("intentos", intentos);
        if (enviadaEn != null) cambio.set("enviadaEn", enviadaEn);
        mongo.updateFirst(new Query(Criteria.where("_id").is(id).and("estado").is(estadoActual)), cambio, AlertaUbicacion.class);
    }
}
