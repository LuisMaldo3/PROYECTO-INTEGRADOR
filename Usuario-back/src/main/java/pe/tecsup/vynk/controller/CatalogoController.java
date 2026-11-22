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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.ActividadesSimilaresService;
import pe.tecsup.vynk.service.FiltrosCatalogo;
import pe.tecsup.vynk.service.UbicacionesActividadesService;

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

    /**
     * US-73: GET /api/catalogo con filtros opcionales por horario (franja=manana|tarde|noche, desde/hasta=HH:mm,
     * fecha=AAAA-MM-DD), categoría (categoria=id) y área (area=una de las cinco). Sin filtros devuelve todo, como antes.
     * Además entrega «filtros» (opciones con su cantidad) y «aplicados».
     */
    @GetMapping
    public Map<String, Object> buscar(@RequestParam(required = false) String franja,
                                      @RequestParam(required = false) String desde,
                                      @RequestParam(required = false) String hasta,
                                      @RequestParam(required = false) String fecha,
                                      @RequestParam(required = false) String categoria,
                                      @RequestParam(required = false) String area) {
        FiltrosCatalogo filtros = FiltrosCatalogo.desde(franja, desde, hasta, fecha, categoria, area);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> todas = (List<Map<String, Object>>) listar().get("actividades");
        List<Map<String, Object>> elegidas = filtros.aplicar(todas);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", elegidas.size());
        respuesta.put("actividades", elegidas);
        respuesta.put("filtros", FiltrosCatalogo.opciones(todas));
        respuesta.put("aplicados", filtros.aplicados());
        return respuesta;
    }

    /** Todas las actividades programadas, sin filtros. La usan otros servicios (agenda sugerida, asistente, etc.). */
    public Map<String, Object> listar() {
        List<Document> docs = mongo.find(new Query(Criteria.where("estado").is("programada")), Document.class, "actividades");
        Map<String, Document> ubicaciones = ubicaciones();
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
        Map<String, Document> ubicaciones = ubicaciones();
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

    private Map<String, Document> ubicaciones() {
        Map<String, Document> lugares = new HashMap<>();
        for (Document u : mongo.find(new Query(), Document.class, "ubicaciones")) lugares.put(String.valueOf(u.get("_id")), u);
        return lugares;
    }

    /** US-70: todas las actividades publicadas de un día (programadas, finalizadas y canceladas), por hora de inicio. */
    public List<Map<String, Object>> delDia(String fecha) {
        List<Document> docs = mongo.find(new Query(Criteria.where("fecha").is(fecha)
                .and("estado").in(ESTADOS_PROGRAMACION)), Document.class, "actividades");
        Map<String, Document> lugares = ubicaciones();
        Map<String, String> categorias = nombresDeCategorias();
        List<Map<String, Object>> actividades = new ArrayList<>();
        for (Document doc : docs) {
            Map<String, Object> a = mapearActividad(doc, lugares);
            Object nombre = categorias.get(String.valueOf(doc.get("categoriaId")));
            if (nombre != null) a.put("categoria", nombre);
            actividades.add(a);
        }
        actividades.sort(Comparator.comparing((Map<String, Object> a) -> String.valueOf(a.getOrDefault("horaInicio", "")))
                .thenComparing(a -> String.valueOf(a.getOrDefault("nombre", ""))));
        return actividades;
    }

    /** US-70: fechas (yyyy-MM-dd) que tienen actividades publicadas, con cuántas hay en cada una. */
    public Map<String, Integer> diasConActividades() {
        Query consulta = new Query(Criteria.where("estado").in(ESTADOS_PROGRAMACION));
        consulta.fields().include("fecha");
        Map<String, Integer> dias = new java.util.TreeMap<>();
        for (Document d : mongo.find(consulta, Document.class, "actividades")) {
            Object fecha = d.get("fecha");
            if (fecha instanceof String f && f.matches("\\d{4}-\\d{2}-\\d{2}")) dias.merge(f, 1, Integer::sum);
        }
        return dias;
    }

    private static final List<String> ESTADOS_PROGRAMACION = List.of("programada", "finalizada", "cancelada");

    private Map<String, Object> mapearActividad(Document doc, Map<String, Document> ubicaciones) {
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
        // US-71: el nombre del lugar y su ficha (sede, pabellón, aula, referencia; coordenadas solo si existen).
        Map<String, Object> ficha = UbicacionesActividadesService.ficha(ubicaciones.get(String.valueOf(doc.get("ubicacionId"))));
        a.put("ubicacion", ficha.get("lugar"));
        a.put("ubicacionReferencia", ficha.get("referencia"));
        a.put("ubicacionDetalle", ficha);
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
