package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.AvisoPush;
import pe.tecsup.vynk.model.Inscripcion;

/**
 * Detecta las actividades canceladas que todavía tienen inscritos.
 *
 * Django cancela la actividad (estado «cancelada») pero deja las inscripciones como «inscrita», así que seguían en la
 * agenda y con recordatorios. Aquí cada inscripción vigente de una actividad cancelada pasa a «cancelada» (con el
 * motivo «actividad_cancelada»): sale de la agenda y se frenan su recordatorio y sus alertas. Si la actividad todavía
 * no había empezado, se le notifica al estudiante. El cambio es condicional, así que cada inscripción se procesa una vez.
 */
@Service
public class DetectorCancelaciones {
    private static final Logger log = LoggerFactory.getLogger(DetectorCancelaciones.class);
    public static final String MOTIVO = "actividad_cancelada";
    /** Actividad sin fecha: la notificación se intenta durante una semana. */
    private static final Duration VIDA_SIN_FECHA = Duration.ofDays(7);

    private final MongoTemplate mongo;
    private final NombresUbicacion ubicaciones;

    public DetectorCancelaciones(MongoTemplate mongo, NombresUbicacion ubicaciones) {
        this.mongo = mongo;
        this.ubicaciones = ubicaciones;
    }

    /** Devuelve cuántas notificaciones de cancelación registró. */
    public int detectar(Instant ahora) {
        Query canceladas = new Query(Criteria.where("estado").is("cancelada"));
        canceladas.fields().include("nombre", "fecha", "horaInicio", "horaFin", "ubicacionId");
        Map<String, Document> porId = new HashMap<>();
        for (Document d : mongo.find(canceladas, Document.class, "actividades")) porId.put(String.valueOf(d.get("_id")), d);
        if (porId.isEmpty()) return 0;
        List<Inscripcion> afectadas = mongo.find(new Query(Criteria.where("estado").is("inscrita")
                .and("actividadId").in(porId.keySet())), Inscripcion.class);
        Map<String, String> lugares = null;
        int avisos = 0;
        for (Inscripcion i : afectadas) {
            Document actividad = porId.get(i.getActividadId());
            try {
                if (!cancelarInscripcion(i, ahora)) continue; // otra ejecución ya la procesó
                Horario horario = Horario.de(actividad.get("fecha"), actividad.get("horaInicio"), actividad.get("horaFin"));
                Instant comienzo = horario.comienzo();
                if (comienzo != null && !comienzo.isAfter(ahora)) continue; // ya pasó: solo sale de la agenda
                if (lugares == null) lugares = ubicaciones.cargar();
                String lugar = NombresUbicacion.nombre(lugares, String.valueOf(actividad.get("ubicacionId")));
                Object nombre = actividad.get("nombre");
                AvisoPush aviso = new AvisoPush();
                aviso.setClave("cancelacion:" + i.getId());
                aviso.setTipo(AvisoPush.CANCELACION);
                aviso.setUsuarioId(i.getEstudianteId());
                aviso.setActividadId(i.getActividadId());
                aviso.setInscripcionId(i.getId());
                aviso.setDatos(MensajeCancelacion.datos(i.getActividadId(),
                        nombre != null ? String.valueOf(nombre) : i.getNombreActividad(), horario, lugar));
                aviso.setExpiraEn(comienzo != null ? comienzo : ahora.plus(VIDA_SIN_FECHA));
                aviso.setCreadoEn(ahora);
                mongo.insert(aviso);
                avisos++;
            } catch (DuplicateKeyException e) {
                log.debug("La cancelación de la inscripción {} ya estaba registrada", i.getId());
            } catch (RuntimeException e) {
                log.error("No se pudo procesar la cancelación de la inscripción {}: {}", i.getId(), e.toString());
            }
        }
        return avisos;
    }

    private boolean cancelarInscripcion(Inscripcion i, Instant ahora) {
        var r = mongo.updateFirst(new Query(Criteria.where("_id").is(i.getId()).and("estado").is("inscrita")),
                new Update().set("estado", "cancelada").set("motivoCancelacion", MOTIVO).set("canceladaEn", ahora),
                Inscripcion.class);
        return r.getModifiedCount() > 0;
    }
}
