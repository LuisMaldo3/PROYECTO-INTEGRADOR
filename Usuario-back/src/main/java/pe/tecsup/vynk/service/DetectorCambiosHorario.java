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
import pe.tecsup.vynk.model.AlertaHorario;
import pe.tecsup.vynk.model.Inscripcion;

/**
 * Detecta que una actividad con inscritos cambió de fecha u hora.
 *
 * Cada inscripción guarda una copia del horario del momento en que el estudiante se inscribió. El administrador edita
 * la actividad desde Django, que no toca esa copia; así, cuando la actividad y la copia no coinciden, el horario cambió.
 * Al detectarlo se registra una alerta para el inscrito y se actualiza la copia, con lo que también quedan al día su
 * agenda y su recordatorio. La actualización es condicional: cada cambio se detecta una sola vez.
 */
@Service
public class DetectorCambiosHorario {
    private static final Logger log = LoggerFactory.getLogger(DetectorCambiosHorario.class);
    private static final Set<String> CERRADAS = Set.of("cancelada", "finalizada");

    private final MongoTemplate mongo;

    public DetectorCambiosHorario(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Devuelve cuántas alertas registró o actualizó. */
    public int detectar(Instant ahora) {
        // Las inscripciones de actividades ya pasadas no interesan; las sin fecha sí (pueden recibir una).
        String ayer = ahora.atZone(ProgramadorRecordatorios.LIMA).toLocalDate().minusDays(1).toString();
        List<Inscripcion> activas = mongo.find(new Query(new Criteria().andOperator(
                Criteria.where("estado").is("inscrita"),
                new Criteria().orOperator(Criteria.where("fecha").gte(ayer), Criteria.where("fecha").in("", null)))),
                Inscripcion.class);
        if (activas.isEmpty()) return 0;
        Map<String, Document> actividades = actividades(activas);
        int alertas = 0;
        for (Inscripcion i : activas) {
            Document actividad = actividades.get(i.getActividadId());
            if (actividad == null || CERRADAS.contains(String.valueOf(actividad.get("estado")).toLowerCase())) continue;
            Horario anterior = new Horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin());
            Horario nuevo = Horario.de(actividad.get("fecha"), actividad.get("horaInicio"), actividad.get("horaFin"));
            // Un horario nuevo incompleto sería un error de datos: no se copia ni se avisa.
            if (nuevo.equals(anterior) || !nuevo.completo()) continue;
            try {
                if (!actualizarCopia(i, nuevo, ahora)) continue; // otra ejecución ya lo detectó
                if (nuevo.comienzo().isAfter(ahora) && registrarAlerta(i, actividad, anterior, nuevo, ahora)) alertas++;
            } catch (RuntimeException e) {
                log.error("No se pudo procesar el cambio de horario de la inscripción {}: {}", i.getId(), e.toString());
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
        consulta.fields().include("fecha", "horaInicio", "horaFin", "estado", "nombre");
        Map<String, Document> porId = new HashMap<>();
        for (Document d : mongo.find(consulta, Document.class, "actividades")) porId.put(String.valueOf(d.get("_id")), d);
        return porId;
    }

    /** Copia el horario nuevo en la inscripción solo si seguía con el anterior. */
    private boolean actualizarCopia(Inscripcion i, Horario nuevo, Instant ahora) {
        var resultado = mongo.updateFirst(new Query(Criteria.where("_id").is(i.getId()).and("estado").is("inscrita")
                        .and("fecha").is(i.getFecha()).and("horaInicio").is(i.getHoraInicio()).and("horaFin").is(i.getHoraFin())),
                new Update().set("fecha", nuevo.fecha()).set("horaInicio", nuevo.inicio()).set("horaFin", nuevo.fin())
                        .set("horarioActualizadoEn", ahora),
                Inscripcion.class);
        return resultado.getModifiedCount() > 0;
    }

    /**
     * Si ya había una alerta sin enviar para esa inscripción, se actualiza con el horario más nuevo (conservando el
     * anterior original); si el horario volvió al de antes, se cancela. Si no, se crea una nueva.
     */
    private boolean registrarAlerta(Inscripcion i, Document actividad, Horario anterior, Horario nuevo, Instant ahora) {
        for (AlertaHorario pendiente : mongo.find(new Query(Criteria.where("inscripcionId").is(i.getId())
                .and("estado").is(AlertaHorario.PENDIENTE)), AlertaHorario.class)) {
            Horario original = new Horario(pendiente.getFechaAnterior(), pendiente.getInicioAnterior(), pendiente.getFinAnterior());
            Update cambio = original.equals(nuevo)
                    ? new Update().set("estado", AlertaHorario.CANCELADA)
                    : new Update().set("fechaNueva", nuevo.fecha()).set("inicioNuevo", nuevo.inicio())
                            .set("finNuevo", nuevo.fin()).set("comienzaEn", nuevo.comienzo()).set("intentos", 0);
            var r = mongo.updateFirst(new Query(Criteria.where("_id").is(pendiente.getId())
                    .and("estado").is(AlertaHorario.PENDIENTE)), cambio, AlertaHorario.class);
            if (r.getModifiedCount() > 0) return !original.equals(nuevo);
        }
        AlertaHorario alerta = new AlertaHorario();
        alerta.setInscripcionId(i.getId());
        alerta.setUsuarioId(i.getEstudianteId());
        alerta.setActividadId(i.getActividadId());
        Object nombre = actividad.get("nombre");
        alerta.setNombreActividad(nombre != null ? String.valueOf(nombre) : i.getNombreActividad());
        alerta.setFechaAnterior(anterior.fecha());
        alerta.setInicioAnterior(anterior.inicio());
        alerta.setFinAnterior(anterior.fin());
        alerta.setFechaNueva(nuevo.fecha());
        alerta.setInicioNuevo(nuevo.inicio());
        alerta.setFinNuevo(nuevo.fin());
        alerta.setComienzaEn(nuevo.comienzo());
        alerta.setCreadaEn(ahora);
        mongo.insert(alerta);
        return true;
    }
}
