package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.AvisoPush;
import pe.tecsup.vynk.repository.AvisoPushRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;

/**
 * Envía las notificaciones de la cola «avisos_push» (cancelaciones y recomendaciones). Cada una se reclama de forma
 * atómica, así sale una sola vez. Justo antes de enviar una recomendación se vuelve a comprobar que siga teniendo
 * sentido: recomendaciones activas, el estudiante no se inscribió y la actividad sigue programada y con cupos.
 */
@Service
public class EnviadorAvisosPush {
    private static final Logger log = LoggerFactory.getLogger(EnviadorAvisosPush.class);
    static final int MAX_INTENTOS = 3;
    private static final Duration ENVIO_ATASCADO = Duration.ofMinutes(5);

    private final AvisoPushRepository avisos;
    private final InscripcionRepository inscripciones;
    private final PreferenciasNotificacionService preferencias;
    private final MongoTemplate mongo;
    private final NotificadorPush push;
    private final EntregaCelulares entrega;

    public EnviadorAvisosPush(AvisoPushRepository avisos, InscripcionRepository inscripciones,
                              DispositivoMovilRepository dispositivos, PreferenciasNotificacionService preferencias,
                              MongoTemplate mongo, NotificadorPush push) {
        this.avisos = avisos;
        this.inscripciones = inscripciones;
        this.preferencias = preferencias;
        this.mongo = mongo;
        this.push = push;
        this.entrega = new EntregaCelulares(dispositivos, push);
    }

    /** Devuelve cuántas notificaciones se entregaron. */
    public int enviarPendientes(Instant ahora) {
        if (!push.disponible()) return 0;
        mongo.updateMulti(new Query(Criteria.where("estado").is(AvisoPush.ENVIANDO)
                        .and("reclamadoEn").lt(ahora.minus(ENVIO_ATASCADO))),
                new Update().set("estado", AvisoPush.PENDIENTE).unset("reclamadoEn"), AvisoPush.class);
        int enviados = 0;
        for (AvisoPush a : avisos.findTop200ByEstadoOrderByCreadoEnAsc(AvisoPush.PENDIENTE)) {
            try {
                if (procesar(a, ahora)) enviados++;
            } catch (RuntimeException e) {
                log.error("Falló la notificación {}: {}", a.getId(), e.toString());
            }
        }
        return enviados;
    }

    private boolean procesar(AvisoPush pendiente, Instant ahora) {
        if (pendiente.getExpiraEn() == null || !pendiente.getExpiraEn().isAfter(ahora)) {
            cerrar(pendiente.getId(), AvisoPush.PENDIENTE, AvisoPush.VENCIDO, null, null);
            return false;
        }
        AvisoPush a = mongo.findAndModify(
                new Query(Criteria.where("_id").is(pendiente.getId()).and("estado").is(AvisoPush.PENDIENTE)),
                new Update().set("estado", AvisoPush.ENVIANDO).set("reclamadoEn", ahora),
                FindAndModifyOptions.options().returnNew(true), AvisoPush.class);
        if (a == null) return false; // otra ejecución lo tomó
        try {
            if (AvisoPush.RECOMENDACION.equals(a.getTipo()) && !recomendacionVigente(a)) {
                cerrar(a.getId(), AvisoPush.ENVIANDO, AvisoPush.DESCARTADO, null, null);
                return false;
            }
            Duration vida = Duration.between(ahora, a.getExpiraEn());
            if (vida.compareTo(Duration.ofMinutes(15)) < 0) vida = Duration.ofMinutes(15);
            switch (entrega.entregar(a.getUsuarioId(), a.getDatos(), vida)) {
                case ENTREGADA -> {
                    cerrar(a.getId(), AvisoPush.ENVIANDO, AvisoPush.ENVIADO, null, ahora);
                    return true;
                }
                // Sin celular registrado: se espera a que abra la app, sin gastar intentos (hasta que venza).
                case SIN_CELULAR -> cerrar(a.getId(), AvisoPush.ENVIANDO, AvisoPush.PENDIENTE, null, null);
                case FALLO -> {
                    int intentos = a.getIntentos() + 1;
                    cerrar(a.getId(), AvisoPush.ENVIANDO,
                            intentos >= MAX_INTENTOS ? AvisoPush.FALLIDO : AvisoPush.PENDIENTE, intentos, null);
                }
            }
            return false;
        } catch (RuntimeException e) {
            cerrar(a.getId(), AvisoPush.ENVIANDO, AvisoPush.PENDIENTE, a.getIntentos() + 1, null);
            throw e;
        }
    }

    private boolean recomendacionVigente(AvisoPush a) {
        if (!preferencias.recomendacionesActivas(a.getUsuarioId())) return false;
        boolean inscrito = inscripciones.findByEstudianteIdAndActividadId(a.getUsuarioId(), a.getActividadId())
                .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).isPresent();
        if (inscrito) return false;
        Object clave = ObjectId.isValid(a.getActividadId()) ? new ObjectId(a.getActividadId()) : a.getActividadId();
        Document actividad = mongo.findOne(new Query(Criteria.where("_id").is(clave)), Document.class, "actividades");
        if (actividad == null || !"programada".equalsIgnoreCase(String.valueOf(actividad.getOrDefault("estado", "programada"))))
            return false;
        Object maximo = actividad.get("maximoParticipantes");
        Object inscritos = actividad.get("participantesInscritos");
        return !(maximo instanceof Number m) || m.intValue() - (inscritos instanceof Number n ? n.intValue() : 0) > 0;
    }

    private void cerrar(String id, String estadoActual, String nuevoEstado, Integer intentos, Instant enviadoEn) {
        Update cambio = new Update().set("estado", nuevoEstado).unset("reclamadoEn");
        if (intentos != null) cambio.set("intentos", intentos);
        if (enviadoEn != null) cambio.set("enviadoEn", enviadoEn);
        mongo.updateFirst(new Query(Criteria.where("_id").is(id).and("estado").is(estadoActual)), cambio, AvisoPush.class);
    }
}
