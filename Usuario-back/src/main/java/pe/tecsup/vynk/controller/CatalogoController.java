package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.ActividadesSimilaresService;

/** Catálogo del estudiante: incluye detalle individual y recomendaciones US-22. */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {
    private final MongoTemplate mongo;
    private final ActividadesSimilaresService similares;

    public CatalogoController(MongoTemplate mongo, ActividadesSimilaresService similares) {
        this.mongo = mongo;
        this.similares = similares;
    }

    @GetMapping
    public Map<String, Object> listar() {
        List<Document> docs = mongo.find(new Query(Criteria.where("estado").is("programada")), Document.class, "actividades");
        Map<String, String> ubicaciones = ubicaciones();
        List<Map<String, Object>> actividades = new ArrayList<>();
        Map<String, String> categorias = nombresDeCategorias();
        for (Document doc : docs) {
            Map<String, Object> a = mapearActividad(doc, ubicaciones);
            Object nombre = categorias.get(String.valueOf(doc.get("categoriaId")));
            if (nombre != null) a.put("categoria", nombre);  // nombre legible; la web nunca debe mostrar el código
            actividades.add(a);
        }
        actividades.sort(Comparator.comparing((Map<String, Object> a) -> String.valueOf(a.getOrDefault("fecha", "")))
                .thenComparing(a -> String.valueOf(a.getOrDefault("horaInicio", ""))));
        return Map.of("total", actividades.size(), "actividades", actividades);
    }

    /** Acceso al detalle incluso si ya terminó, pero nunca a actividades canceladas o privadas. */
    @GetMapping("/{id}")
    public Map<String, Object> detalle(@PathVariable String id) {
        return mapearActividad(buscarVisible(id), ubicaciones());
    }

    @GetMapping("/{id}/similares")
    public Map<String, Object> similares(@PathVariable String id) {
        Document origen = buscarVisible(id);
        Map<String, String> ubicaciones = ubicaciones();
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Document doc : similares.buscar(origen, 4)) lista.add(mapearActividad(doc, ubicaciones));
        return Map.of("actividadId", id, "total", lista.size(), "actividades", lista);
    }

    private Document buscarVisible(String id) {
        if (id == null || id.length() > 80 || id.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido");
        Object key = ObjectId.isValid(id) ? new ObjectId(id) : id;
        Document doc = mongo.findOne(new Query(Criteria.where("_id").is(key)), Document.class, "actividades");
        if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Actividad no encontrada");
        String estado = String.valueOf(doc.getOrDefault("estado", ""));
        if (!"programada".equalsIgnoreCase(estado) && !"finalizada".equalsIgnoreCase(estado))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La actividad no está publicada");
        return doc;
    }

    private Map<String, String> ubicaciones() {
        Map<String, String> nombres = new HashMap<>();
        for (Document u : mongo.find(new Query(), Document.class, "ubicaciones"))
            nombres.put(String.valueOf(u.get("_id")), String.valueOf(u.getOrDefault("nombre", "")));
        return nombres;
    }

    private Map<String, Object> mapearActividad(Document doc, Map<String, String> ubicaciones) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", String.valueOf(doc.get("_id")));
        for (String campo : List.of("nombre", "descripcion", "areaTecsup", "fecha", "horaInicio", "horaFin",
                "estado", "maximoParticipantes", "tipoActividad", "modalidad", "maxIntegrantesEquipo",
                "reglasCompetencia", "puntosPorPuesto", "categoriaId", "ubicacionId", "categoriaGenero")) a.put(campo, doc.get(campo));
        a.put("encargadosPorArea", doc.getOrDefault("encargadosPorArea", Map.of()));
        Object portada = doc.get("portada");
        if (portada instanceof Map<?, ?> mapa) {
            Map<String, Object> p = new LinkedHashMap<>();
            for (String campo : List.of("tipo", "url", "archivoId", "autor", "licencia", "creditoUrl"))
                if (mapa.get(campo) != null) p.put(campo, mapa.get(campo));
            a.put("portada", p);
            Object archivo = p.get("archivoId");
            a.put("portadaUrl", List.of("gemini", "subida").contains(p.get("tipo")) && archivo != null
                    ? "/api/portadas/archivo/" + archivo : p.get("url"));
        } else { a.put("portada", null); a.put("portadaUrl", null); }
        Object requisitos = doc.get("requisitos");
        if (requisitos == null) requisitos = doc.get("requisitosInscripcion");
        a.put("requisitos", requisitos);
        a.put("participantesInscritos", doc.get("participantesInscritos") instanceof Number n ? n.intValue() : 0);
        Object max = doc.get("maximoParticipantes");
        int inscritos = (int) a.get("participantesInscritos");
        a.put("cuposDisponibles", max instanceof Number n ? Math.max(0, n.intValue() - inscritos) : null);
        a.put("unidadCupo", "competencia".equals(doc.get("tipoActividad")) && "equipos".equals(doc.get("modalidad")) ? "equipos" : "estudiantes");
        a.put("ubicacion", ubicaciones.getOrDefault(String.valueOf(doc.get("ubicacionId")), "Ubicación por confirmar"));
        Object org = doc.get("organizador");
        a.put("organizador", org instanceof Map<?, ?> m ? String.valueOf(m.get("nombre") == null ? "" : m.get("nombre")) : org == null ? "" : String.valueOf(org));
        return a;
    }

    /** id → nombre de cada categoría («deportes» → «Deportes»). */
    private Map<String, String> nombresDeCategorias() {
        Map<String, String> m = new java.util.HashMap<>();
        for (Document c : mongo.findAll(Document.class, "categorias")) {
            Object nombre = c.get("nombre");
            if (nombre != null) m.put(String.valueOf(c.get("_id")), String.valueOf(nombre));
        }
        return m;
    }
}
