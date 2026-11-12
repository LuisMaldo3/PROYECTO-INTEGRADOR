package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.time.Instant;
import java.util.Date;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-07: solo resultados validados/publicados. La tabla sumatoria se hará en Sprint 3. */
@RestController
@RequestMapping("/api/competencias")
public class ResultadosPublicosController {
    private static final List<String> AREAS = List.of("Gestión y Producción", "Tecnología Digital",
            "Minería y Procesos Químicos", "Electricidad y Electrónica", "Mecánica");
    private final MongoTemplate mongo;
    public ResultadosPublicosController(MongoTemplate mongo) { this.mongo = mongo; }

    @GetMapping("/resultados")
    public Map<String, Object> resultados() {
        List<Document> publicados = mongo.find(new Query(Criteria.where("estado").is("publicado")), Document.class, "resultados_competencias");
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Document r : publicados) {
            String id = String.valueOf(r.get("actividadId"));
            Object key = org.bson.types.ObjectId.isValid(id) ? new org.bson.types.ObjectId(id) : id;
            Document actividad = mongo.findOne(new Query(Criteria.where("_id").is(key)), Document.class, "actividades");
            if (actividad == null || "cancelada".equals(actividad.get("estado"))) continue;
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("actividadId", id); dto.put("nombre", actividad.get("nombre"));
            dto.put("fecha", actividad.get("fecha")); dto.put("modalidad", actividad.get("modalidad"));
            dto.put("estadoActividad", actividad.getOrDefault("estado", ""));
            Object portada = actividad.get("portada");
            if (portada instanceof Map<?, ?> p) {
                Object archivo = p.get("archivoId");
                dto.put("portadaUrl", ("gemini".equals(p.get("tipo")) || "subida".equals(p.get("tipo"))) && archivo != null
                        ? "/api/portadas/archivo/" + archivo : p.get("url"));
                // Mantener autor/créditos de las imágenes de Commons.
                Map<String,Object> publica = new LinkedHashMap<>();
                for (String campo : List.of("tipo", "url", "autor", "licencia", "creditoUrl"))
                    if (p.get(campo) != null) publica.put(campo, p.get(campo));
                dto.put("portada", publica);
            } else { dto.put("portadaUrl", null); dto.put("portada", null); }
            // No exponer IDs internos de participantes, solo el nombre y el área verificada.
            List<Map<String,Object>> posiciones = new ArrayList<>();
            Object raw = r.get("posiciones");
            if (raw instanceof List<?> items) for (Object x : items) if (x instanceof Map<?, ?> p) {
                Map<String,Object> puesto = new LinkedHashMap<>();
                puesto.put("puesto", p.get("puesto")); puesto.put("nombre", p.get("nombre"));
                puesto.put("puntos", p.get("puntos"));
                puesto.put("areaTecsup", p.get("areaTecsup"));
                posiciones.add(puesto);
            }
            dto.put("posiciones", posiciones);
            lista.add(dto);
        }
        lista.sort(Comparator.comparing((Map<String, Object> a) -> String.valueOf(a.getOrDefault("fecha", ""))).reversed());
        return Map.of("total", lista.size(), "resultados", lista);
    }

    /** US-22-T07: detalle oficial por competencia, jamás devuelve borradores. */
    @GetMapping("/resultados/{actividadId}")
    public Map<String, Object> resultadoPorActividad(@PathVariable String actividadId) {
        if (actividadId == null || actividadId.isBlank() || actividadId.length() > 80)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID inválido");
        for (Map<String, Object> resultado : (List<Map<String, Object>>) resultados().get("resultados")) {
            if (actividadId.equals(resultado.get("actividadId")))
                return Map.of("estado", "publicado", "resultado", resultado);
        }
        return Map.of("estado", "sin_resultados", "resultado", Map.of());
    }

    /** US-40-T09/T10: clasificación visible al estudiante autenticado.
     * La lógica de validación de puestos/puntos y selección de publicación
     * coincide con US-39/US-40 de Django. Nunca escribe puntajes en MongoDB.
     */
    @GetMapping("/clasificacion")
    public Map<String, Object> clasificacion() {
        Map<String, Long> puntos = new LinkedHashMap<>();
        Map<String, Integer> primeros = new HashMap<>();
        Map<String, List<Map<String, Object>>> desglose = new HashMap<>();
        for (String area : AREAS) {
            puntos.put(area, 0L);
            primeros.put(area, 0);
            desglose.put(area, new ArrayList<>());
        }
        // Si existen publicaciones históricas duplicadas, usar la más reciente.
        List<Document> oficiales = mongo.find(new Query(Criteria.where("estado").is("publicado")),
                Document.class, "resultados_competencias");
        Map<String, Document> unicos = new HashMap<>();
        for (Document doc : oficiales) {
            String id = String.valueOf(doc.get("actividadId"));
            Document anterior = unicos.get(id);
            if (anterior == null || compararPublicacion(doc, anterior) > 0) unicos.put(id, doc);
        }
        int competencias = 0;
        for (Map.Entry<String, Document> entrada : unicos.entrySet()) {
            String actividadId = entrada.getKey();
            Object key = org.bson.types.ObjectId.isValid(actividadId)
                    ? new org.bson.types.ObjectId(actividadId) : actividadId;
            Document actividad = mongo.findOne(new Query(Criteria.where("_id").is(key)),
                    Document.class, "actividades");
            if (actividad == null || "cancelada".equals(actividad.getString("estado"))
                    || !"competencia".equals(actividad.getString("tipoActividad"))) continue;
            competencias++;
            Set<String> inscripcionesVistas = new HashSet<>();
            Set<Integer> puestosVistos = new HashSet<>();
            Document resultado = entrada.getValue();
            if (!(resultado.get("posiciones") instanceof List<?> posiciones)) continue;
            for (Object raw : posiciones) {
                if (!(raw instanceof Map<?, ?> pos)) continue;
                String inscripcionId = String.valueOf(pos.get("inscripcionId"));
                String area = String.valueOf(pos.get("areaTecsup"));
                Object puntaje = pos.get("puntos");
                Object orden = pos.get("puesto");
                if (inscripcionId.isBlank() || "null".equals(inscripcionId)
                        || !(orden instanceof Integer puesto) || puesto < 1 || puesto > 100
                        || !(puntaje instanceof Integer || puntaje instanceof Long)
                        || !puntos.containsKey(area)) continue;
                long valor = ((Number) puntaje).longValue();
                if (valor < 0 || valor > 10000 || inscripcionesVistas.contains(inscripcionId)
                        || puestosVistos.contains(puesto)) continue;
                inscripcionesVistas.add(inscripcionId);
                puestosVistos.add(puesto);
                puntos.put(area, puntos.get(area) + valor);
                if (puesto == 1) primeros.put(area, primeros.get(area) + 1);
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("actividadId", actividadId);
                fila.put("competencia", actividad.get("nombre"));
                fila.put("puesto", puesto);
                fila.put("puntos", valor);
                desglose.get(area).add(fila);
            }
        }
        List<Map<String, Object>> areas = new ArrayList<>();
        for (String area : AREAS) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("area", area);
            fila.put("puntos", puntos.get(area));
            fila.put("primerosPuestos", primeros.get(area));
            fila.put("desglose", desglose.get(area));
            areas.add(fila);
        }
        areas.sort(Comparator.comparingLong((Map<String, Object> a) -> ((Number) a.get("puntos")).longValue())
                .reversed().thenComparing(Comparator.comparingInt(
                        (Map<String, Object> a) -> ((Number) a.get("primerosPuestos")).intValue()).reversed())
                .thenComparing(a -> String.valueOf(a.get("area"))));
        for (int i = 0; i < areas.size(); i++) areas.get(i).put("posicion", i + 1);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", competencias > 0 ? "publicado" : "sin_resultados");
        respuesta.put("totalCompetencias", competencias);
        respuesta.put("criterioDesempate", "Más puntos, más primeros puestos y orden alfabético");
        // Ganador general: solo cuando el administrador cerró la T&C Week (colección cierre_tcweek).
        Document cierre = mongo.findOne(new Query(Criteria.where("_id").is("tcweek")), Document.class, "cierre_tcweek");
        if (cierre != null && "cerrada".equals(cierre.getString("estado")) && cierre.get("ganador") instanceof Document g) {
            Map<String, Object> ganador = new LinkedHashMap<>();
            ganador.put("area", g.get("area")); ganador.put("puntos", g.get("puntos")); ganador.put("departamento", g.get("departamento"));
            respuesta.put("ganadorGeneral", ganador);
        }
        respuesta.put("areas", areas);
        return respuesta;
    }

    private static int compararPublicacion(Document nuevo, Document anterior) {
        int fechas = fechaPublicacion(nuevo).compareTo(fechaPublicacion(anterior));
        if (fechas != 0) return fechas;
        return String.valueOf(nuevo.get("_id")).compareTo(String.valueOf(anterior.get("_id")));
    }

    private static String fechaPublicacion(Document doc) {
        Object fecha = doc.get("publicadoEn");
        if (fecha instanceof Date d) return d.toInstant().toString();
        if (fecha instanceof Instant i) return i.toString();
        return fecha instanceof String s ? s : "";
    }
}
