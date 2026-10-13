package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lee directamente las actividades creadas por Admin-back en la misma base MongoDB. */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {
    private final MongoTemplate mongo;

    public CatalogoController(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @GetMapping
    public Map<String, Object> listar() {
        Query query = new Query(Criteria.where("estado").nin("cancelada", "inactiva"));
        List<Document> docs = mongo.find(query, Document.class, "actividades");
        List<Map<String, Object>> actividades = new ArrayList<>();
        for (Document doc : docs) {
            String estado = String.valueOf(doc.getOrDefault("estado", "programada"));
            if (!"programada".equalsIgnoreCase(estado)) continue;
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", String.valueOf(doc.get("_id")));
            a.put("nombre", doc.get("nombre"));
            a.put("descripcion", doc.get("descripcion"));
            a.put("fecha", doc.get("fecha"));
            a.put("horaInicio", doc.get("horaInicio"));
            a.put("horaFin", doc.get("horaFin"));
            a.put("estado", estado);
            a.put("maximoParticipantes", doc.get("maximoParticipantes"));
            int inscritos = doc.get("participantesInscritos") instanceof Number n ? n.intValue() : 0;
            a.put("participantesInscritos", inscritos);
            Object max = doc.get("maximoParticipantes");
            a.put("cuposDisponibles", max instanceof Number n ? Math.max(0, n.intValue() - inscritos) : null);
            a.put("categoriaId", doc.get("categoriaId"));
            a.put("ubicacionId", doc.get("ubicacionId"));
            a.put("organizador", doc.get("organizador"));
            actividades.add(a);
        }
        actividades.sort(Comparator.comparing((Map<String, Object> a) -> String.valueOf(a.getOrDefault("fecha", "")))
                .thenComparing(a -> String.valueOf(a.getOrDefault("horaInicio", ""))));
        return Map.of("total", actividades.size(), "actividades", actividades);
    }
}
