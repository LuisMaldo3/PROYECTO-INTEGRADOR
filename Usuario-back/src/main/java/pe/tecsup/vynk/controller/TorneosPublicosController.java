package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * T&C ARENA y Karadúo para estudiantes: enfrentamientos, horarios, marcadores y campeones.
 * SOLO LECTURA: los torneos los crea y los modifica el panel administrativo (Admin-back).
 * No se expone la auditoría ni quién registró cada resultado. Los departamentos viajan como
 * identificador (quimica, mecanica, td, electricidad, dpg); cada cliente pone nombre, color y escudo.
 */
@RestController
@RequestMapping("/api/competencias")
public class TorneosPublicosController {
    private final MongoTemplate mongo;

    public TorneosPublicosController(MongoTemplate mongo) { this.mongo = mongo; }

    @GetMapping("/torneos")
    public Map<String, Object> torneos(@RequestParam(required = false) String tipo) {
        Criteria criterio = Criteria.where("estado").ne("anulado");
        if ("arena".equals(tipo) || "karaduo".equals(tipo)) criterio = criterio.and("tipo").is(tipo);
        List<Document> docs = mongo.find(new Query(criterio), Document.class, "torneos");
        docs.sort((a, b) -> String.valueOf(a.get("deporte")).compareToIgnoreCase(String.valueOf(b.get("deporte"))));

        List<Map<String, Object>> salida = new ArrayList<>();
        for (Document d : docs) salida.add(publico(d));
        return Map.of("torneos", salida);
    }

    /** id → participante (equipo o alumno). Acepta torneos antiguos cuyos participantes eran solo departamentos. */
    private static Map<String, Map<String, Object>> mapa(Document d) {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        Object lista = d.get("participantes");
        if (lista instanceof List<?> l) for (Object o : l) {
            Map<String, Object> e = new LinkedHashMap<>();
            if (o instanceof Map<?, ?> x) {
                e.put("id", String.valueOf(x.get("id"))); e.put("nombre", x.get("nombre"));
                e.put("area", x.get("area")); e.put("departamento", x.get("departamento"));
            } else { e.put("id", String.valueOf(o)); e.put("nombre", String.valueOf(o)); e.put("area", ""); e.put("departamento", String.valueOf(o)); }
            m.put(String.valueOf(e.get("id")), e);
        }
        return m;
    }

    private static Object departamento(Map<String, Map<String, Object>> m, Object id) {
        Map<String, Object> e = id == null ? null : m.get(String.valueOf(id));
        return e == null ? null : e.get("departamento");
    }

    private static Map<String, Object> publico(Document d) {
        List<Document> partidos = d.getList("partidos", Document.class, List.of());
        int ultimaRonda = partidos.stream().mapToInt(p -> p.getInteger("ronda", 1)).max().orElse(1);
        Document finalDoc = partidos.stream()
                .filter(p -> p.getInteger("ronda", 1) == ultimaRonda && !Boolean.TRUE.equals(p.getBoolean("tercerPuesto")))
                .findFirst().orElse(new Document());
        boolean campeonValidado = Boolean.TRUE.equals(finalDoc.getBoolean("validado"));
        Map<String, Map<String, Object>> m = mapa(d);

        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", String.valueOf(d.get("_id")));
        t.put("tipo", d.getString("tipo"));
        t.put("deporte", d.getString("deporte"));
        t.put("modoResultado", d.getString("modoResultado"));
        t.put("setsParaGanar", d.get("setsParaGanar"));
        t.put("estado", d.getString("estado"));
        t.put("equipos", new ArrayList<>(m.values()));
        // «participantes», «campeon», «a», «b» y «ganador» siguen siendo departamentos para la app ya instalada;
        // los equipos se identifican con «aId», «bId», «ganadorId» y «campeonId».
        List<Object> deps = new ArrayList<>();
        for (Map<String, Object> e : m.values()) deps.add(e.get("departamento"));
        t.put("participantes", deps);
        Object campeon = campeonValidado ? d.get("campeon") : null, sub = campeonValidado ? d.get("subcampeon") : null;
        t.put("campeonId", campeon); t.put("subcampeonId", sub);
        t.put("campeon", departamento(m, campeon)); t.put("subcampeon", departamento(m, sub));
        t.put("campeonValidado", campeonValidado);
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Document p : partidos) lista.add(partido(p, m));
        t.put("partidos", lista);
        return t;
    }

    private static Map<String, Object> partido(Document p, Map<String, Map<String, Object>> equipos) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String campo : List.of("id", "ronda", "posicion", "nombreRonda", "estado",
                "descanso", "tercerPuesto", "fecha", "hora", "lugar")) m.put(campo, p.get(campo));
        m.put("aId", p.get("a")); m.put("bId", p.get("b")); m.put("ganadorId", p.get("ganador"));
        m.put("a", departamento(equipos, p.get("a"))); m.put("b", departamento(equipos, p.get("b")));
        m.put("ganador", departamento(equipos, p.get("ganador")));
        boolean validado = Boolean.TRUE.equals(p.getBoolean("validado"));
        m.put("provisional", p.get("resultado") != null && !validado);
        Document r = p.get("resultado", Document.class);
        if (r != null) {
            Map<String, Object> res = new LinkedHashMap<>();
            for (String campo : List.of("modo", "a", "b", "setsA", "setsB", "ganadorPenales", "observaciones"))
                if (r.containsKey(campo)) res.put(campo, r.get(campo));
            m.put("resultado", res);
        } else {
            m.put("resultado", null);
        }
        return m;
    }
}
