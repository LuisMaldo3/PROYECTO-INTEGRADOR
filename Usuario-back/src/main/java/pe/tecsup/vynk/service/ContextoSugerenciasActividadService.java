package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.ComentarioActividad;

/**
 * Tarea 1 — Reunir los datos de la actividad.
 * Junta, de una sola actividad ya finalizada o en curso, lo que hace falta para pedirle a Gemini
 * sugerencias de mejora de cara a futuras actividades similares: participación (reutiliza US-60/61,
 * sin duplicar ese cálculo), comentarios de los inscritos (desidentificados, igual que en US-62/63) y,
 * si es una competencia con resultado ya publicado, cuántos puestos se definieron y quién fue el
 * ganador. No decide nada ni llama a Gemini: solo junta datos reales, de solo lectura.
 */
@Service
public class ContextoSugerenciasActividadService {
    private static final int MAX_COMENTARIOS = 40;

    private final OrganizadorParticipacionService participacion;
    private final ComentariosActividadService comentarios;
    private final MongoTemplate mongo;

    public ContextoSugerenciasActividadService(OrganizadorParticipacionService participacion,
            ComentariosActividadService comentarios, MongoTemplate mongo) {
        this.participacion = participacion;
        this.comentarios = comentarios;
        this.mongo = mongo;
    }

    /** Lo que se le muestra al organizador y lo que se le manda a Gemini es el mismo objeto: nada oculto. */
    public Map<String, Object> reunir(Document actividad) {
        String actividadId = String.valueOf(actividad.get("_id"));
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("actividadId", actividadId);
        datos.put("nombreActividad", String.valueOf(actividad.getOrDefault("nombre", "Actividad")));
        datos.put("tipoActividad", String.valueOf(actividad.getOrDefault("tipoActividad", "actividad")));
        datos.put("categoria", actividad.get("categoria"));
        datos.put("fecha", actividad.get("fecha"));

        datos.put("participacion", participacion.calcular(actividad));

        List<ComentarioActividad> todos = comentarios.listar(actividadId);
        List<String> textos = new ArrayList<>();
        for (ComentarioActividad c : todos) {
            if (c.getTexto() == null || c.getTexto().isBlank()) continue;
            textos.add(GeminiComentariosService.anonimizar(c.getTexto()));
            if (textos.size() >= MAX_COMENTARIOS) break;
        }
        datos.put("totalComentarios", todos.size());
        datos.put("comentarios", textos);

        Map<String, Object> resultado = resultadoOficial(actividadId, actividad);
        if (resultado != null) datos.put("resultadoOficial", resultado);

        return datos;
    }

    /** Solo si la competencia ya tiene un resultado PUBLICADO (nunca una propuesta pendiente). */
    private Map<String, Object> resultadoOficial(String actividadId, Document actividad) {
        if (!"competencia".equals(String.valueOf(actividad.get("tipoActividad")))) return null;
        Document doc = mongo.findOne(new Query(Criteria.where("actividadId").is(actividadId)),
                Document.class, "resultados_competencias");
        if (doc == null || !"publicado".equals(doc.getString("estado"))) return null;
        Object crudas = doc.get("posiciones");
        if (!(crudas instanceof List<?> posiciones) || posiciones.isEmpty()) return null;
        Map<String, Object> primero = posiciones.get(0) instanceof Document d ? d : null;
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalPuestos", posiciones.size());
        resumen.put("ganador", primero == null ? null : primero.get("nombre"));
        return resumen;
    }
}
