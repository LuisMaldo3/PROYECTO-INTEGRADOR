package pe.tecsup.vynk.controller;

import java.util.ArrayList;
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

    /** Sprint 3: los totales se RECONSTRUYEN desde resultados publicados (sin doble conteo).
     * No se aceptan puntos de la web, resultados pendientes ni afiliaciones sin verificar. */
    @GetMapping("/clasificacion")
    public Map<String, Object> clasificacion() {
        Map<String, Long> puntos = new LinkedHashMap<>();
        Map<String, Integer> primeros = new HashMap<>();
        Map<String, List<Map<String,Object>>> desglose = new HashMap<>();
        for (String area : AREAS) {
            puntos.put(area, 0L); primeros.put(area, 0); desglose.put(area, new ArrayList<>());
        }
        Set<String> actividadesProcesadas = new HashSet<>();
        List<Document> oficiales = mongo.find(new Query(Criteria.where("estado").is("publicado")),
                Document.class, "resultados_competencias");
        int competencias = 0;
        for (Document resultado : oficiales) {
            String actividadId = String.valueOf(resultado.get("actividadId"));
            if (!actividadesProcesadas.add(actividadId)) continue;
            Object key = org.bson.types.ObjectId.isValid(actividadId)
                    ? new org.bson.types.ObjectId(actividadId) : actividadId;
            Document actividad = mongo.findOne(new Query(Criteria.where("_id").is(key)), Document.class, "actividades");
            if (actividad == null || "cancelada".equals(actividad.getString("estado"))) continue;
            if (!"competencia".equals(actividad.getString("tipoActividad"))) continue;
            if (!(resultado.get("posiciones") instanceof List<?> posiciones)) continue;
            competencias++;
            for (Object raw : posiciones) {
                if (!(raw instanceof Map<?, ?> p)) continue;
                String area = String.valueOf(p.get("areaTecsup"));
                if (!puntos.containsKey(area) || !(p.get("puntos") instanceof Number score)) continue;
                int numero = score.intValue();
                if (numero < 0 || numero > 10000) continue;
                puntos.put(area, puntos.get(area) + numero);
                int puesto = p.get("puesto") instanceof Number n ? n.intValue() : 0;
                if (puesto == 1) primeros.put(area, primeros.get(area) + 1);
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("actividadId", actividadId); fila.put("competencia", actividad.get("nombre"));
                fila.put("puesto", puesto); fila.put("puntos", numero);
                desglose.get(area).add(fila);
            }
        }
        List<Map<String, Object>> areas = new ArrayList<>();
        for (String nombre : AREAS) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("area", nombre); a.put("puntos", puntos.get(nombre));
            a.put("primerosPuestos", primeros.get(nombre)); a.put("desglose", desglose.get(nombre));
            areas.add(a);
        }
        areas.sort(Comparator.comparingLong((Map<String,Object> a) -> ((Number) a.get("puntos")).longValue()).reversed()
                .thenComparing(Comparator.comparingInt((Map<String,Object> a) -> ((Number) a.get("primerosPuestos")).intValue()).reversed())
                .thenComparing(a -> String.valueOf(a.get("area"))));
        for (int i = 0; i < areas.size(); i++) areas.get(i).put("posicion", i + 1);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("estado", competencias > 0 ? "publicado" : "sin_resultados");
        respuesta.put("totalCompetencias", competencias);
        respuesta.put("criterioDesempate", "Mayor puntaje; luego más primeros puestos; finalmente orden alfabético");
        respuesta.put("areas", areas);
        return respuesta;
    }
}
