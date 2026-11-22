package pe.tecsup.vynk.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.AlertaUbicacion;
import pe.tecsup.vynk.model.Inscripcion;

/**
 * Detecta que una actividad con inscritos cambió de lugar.
 *
 * Igual que con el horario: cada inscripción guarda el identificador de la ubicación del momento en que el estudiante
 * se inscribió, y Django solo cambia «ubicacionId» en la actividad. Si no coinciden, el lugar cambió: se registra una
 * alerta con la ubicación anterior y la nueva (con sus nombres) y se actualiza la inscripción, una sola vez por cambio.
 */
@Service
public class DetectorCambiosUbicacion {
    private static final Logger log = LoggerFactory.getLogger(DetectorCambiosUbicacion.class);
    private static final Set<String> CERRADAS = Set.of("cancelada", "finalizada");

    private final MongoTemplate mongo;
    private final NombresUbicacion ubicaciones;

    public DetectorCambiosUbicacion(MongoTemplate mongo, NombresUbicacion ubicaciones) {
        this.mongo = mongo;
        this.ubicaciones = ubicaciones;
    }

    /** Devuelve cuántas alertas registró o actualizó. */
    public int detectar(Instant ahora) {
        String ayer = ahora.atZone(ProgramadorRecordatorios.LIMA).toLocalDate().minusDays(1).toString();
        List<Inscripcion> activas = mongo.find(new Query(new Criteria().andOperator(
                Criteria.where("estado").is("inscrita"),
                new Criteria().orOperator(Criteria.where("fecha").gte(ayer), Criteria.where("fecha").in("", null)))),
                Inscripcion.class);
        if (activas.isEmpty()) return 0;
        Map<String, Document> actividades = actividades(activas);
        Map<String, String> nombres = null; // se cargan solo si hay algún cambio
        int alertas = 0;
        for (Inscripcion i : activas) {
            Document actividad = actividades.get(i.getActividadId());
            if (actividad == null || CERRADAS.contains(String.valueOf(actividad.get("estado")).toLowerCase())) continue;
            String anterior = limpio(i.getUbicacion());
            String nueva = limpio(actividad.get("ubicacionId"));
            // Una actividad sin ubicación sería un error de datos: no se copia ni se avisa.
            if (nueva.isEmpty() || nueva.equals(anterior)) continue;
            try {
                if (!actualizarCopia(i, nueva, ahora)) continue; // otra ejecución ya lo detectó
                Instant comienzo = Horario.de(actividad.get("fecha"), actividad.get("horaInicio"), actividad.get("horaFin")).comienzo();
                if (comienzo != null && !comienzo.isAfter(ahora)) continue; // ya empezó: solo se actualiza
                if (nombres == null) nombres = ubicaciones.cargar();
                if (registrarAlerta(i, actividad, anterior, nueva, nombres, comienzo, ahora)) alertas++;
            } catch (RuntimeException e) {
                log.error("No se pudo procesar el cambio de ubicación de la inscripción {}: {}", i.getId(), e.toString());
            }
        }
        return alertas;
    }

    private Map<String, Document> actividades(List<Inscripcion> inscripciones) {
        Set<Object> ids = new LinkedHashSet<>();
        for (Inscripcion i : inscripciones) {
            String id = i.getActividadId();
            if (id == null || id.isBlank()) continue;
            ids.add(id);
            if (ObjectId.isValid(id)) ids.add(new ObjectId(id));
        }
        Query consulta = new Query(Criteria.where("_id").in(ids));
        consulta.fields().include("ubicacionId", "fecha", "horaInicio", "horaFin", "estado", "nombre");
        Map<String, Document> porId = new HashMap<>();
        for (Document d : mongo.find(consulta, Document.class, "actividades")) porId.put(String.valueOf(d.get("_id")), d);
        return porId;
    }

    /** Copia la ubicación nueva en la inscripción solo si seguía con la anterior. */
    private boolean actualizarCopia(Inscripcion i, String nueva, Instant ahora) {
        var resultado = mongo.updateFirst(new Query(Criteria.where("_id").is(i.getId()).and("estado").is("inscrita")
                        .and("ubicacion").is(i.getUbicacion())),
                new Update().set("ubicacion", nueva).set("ubicacionActualizadaEn", ahora), Inscripcion.class);
        return resultado.getModifiedCount() > 0;
    }

    /**
     * Si ya había una alerta sin enviar, se actualiza con el lugar más nuevo (conservando el anterior original);
     * si el lugar volvió al de antes, se cancela. Si no, se crea una nueva.
     */
    private boolean registrarAlerta(Inscripcion i, Document actividad, String anterior, String nueva,
                                    Map<String, String> nombres, Instant comienzo, Instant ahora) {
        String nombreNueva = NombresUbicacion.nombre(nombres, nueva);
        for (AlertaUbicacion pendiente : mongo.find(new Query(Criteria.where("inscripcionId").is(i.getId())
                .and("estado").is(AlertaUbicacion.PENDIENTE)), AlertaUbicacion.class)) {
            boolean volvio = limpio(pendiente.getUbicacionAnteriorId()).equals(nueva);
            Update cambio = volvio
                    ? new Update().set("estado", AlertaUbicacion.CANCELADA)
                    : new Update().set("ubicacionNuevaId", nueva).set("ubicacionNueva", nombreNueva)
                            .set("comienzaEn", comienzo).set("intentos", 0);
            var r = mongo.updateFirst(new Query(Criteria.where("_id").is(pendiente.getId())
                    .and("estado").is(AlertaUbicacion.PENDIENTE)), cambio, AlertaUbicacion.class);
            if (r.getModifiedCount() > 0) return !volvio;
        }
        AlertaUbicacion alerta = new AlertaUbicacion();
        alerta.setInscripcionId(i.getId());
        alerta.setUsuarioId(i.getEstudianteId());
        alerta.setActividadId(i.getActividadId());
        Object nombre = actividad.get("nombre");
        alerta.setNombreActividad(nombre != null ? String.valueOf(nombre) : i.getNombreActividad());
        alerta.setUbicacionAnteriorId(anterior);
        alerta.setUbicacionNuevaId(nueva);
        alerta.setUbicacionAnterior(NombresUbicacion.nombre(nombres, anterior));
        alerta.setUbicacionNueva(nombreNueva);
        alerta.setComienzaEn(comienzo);
        alerta.setCreadaEn(ahora);
        mongo.insert(alerta);
        return true;
    }

    static String limpio(Object valor) {
        String s = valor == null ? "" : String.valueOf(valor).strip();
        return "null".equals(s) ? "" : s;
    }
}
