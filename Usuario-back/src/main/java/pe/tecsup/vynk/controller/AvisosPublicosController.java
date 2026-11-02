package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Avisos para el inicio web y Android: únicamente publicados, más recientes primero.
 * React y Kotlin no necesitan consumir los endpoints administrativos de Django.
 */
@RestController
@RequestMapping("/api/avisos/publicados")
public class AvisosPublicosController {
    private final MongoTemplate mongo;

    public AvisosPublicosController(MongoTemplate mongo) { this.mongo = mongo; }

    private static String iso(Object date) {
        if (date instanceof Date value) return value.toInstant().toString();
        if (date instanceof Instant value) return value.toString();
        return date == null ? "" : String.valueOf(date);
    }

    private static Instant orden(Object value) {
        if (value instanceof Date date) return date.toInstant();
        if (value instanceof Instant date) return date;
        try { return Instant.parse(String.valueOf(value)); }
        catch (Exception e) { return Instant.EPOCH; }
    }

    @GetMapping
    public Map<String, Object> listar() {
        Date ahora = new Date();
        // Reutilizar la misma transición de los avisos programados de Django.
        mongo.updateMulti(new Query(new Criteria().andOperator(
                Criteria.where("estado").is("programado"), Criteria.where("publicarEn").lte(ahora))),
                new Update().set("estado", "publicado").set("publicadoEn", ahora), "avisos");
        Query consulta = new Query(new Criteria().orOperator(
                Criteria.where("estado").is("publicado"), Criteria.where("estado").exists(false)));
        consulta.limit(600);
        List<Document> documentos = mongo.find(consulta, Document.class, "avisos");
        documentos.sort(Comparator.comparing(
                (Document d) -> orden(d.getOrDefault("publicadoEn", d.get("creadoEn")))).reversed());
        List<Map<String, Object>> avisos = new ArrayList<>();
        for (Document doc : documentos) {
            Map<String, Object> aviso = new LinkedHashMap<>();
            aviso.put("id", String.valueOf(doc.get("_id")));
            aviso.put("titulo", doc.getOrDefault("titulo", "Comunicado"));
            aviso.put("mensaje", doc.getOrDefault("mensaje", ""));
            aviso.put("publicadoEn", iso(doc.getOrDefault("publicadoEn", doc.get("creadoEn"))));
            Object portada = doc.get("portada");
            if (portada instanceof Map<?, ?> imagen) {
                Map<String, Object> segura = new LinkedHashMap<>();
                for (String k : List.of("tipo", "url", "archivoId", "autor", "licencia", "creditoUrl")) {
                    if (imagen.get(k) != null) segura.put(k, imagen.get(k));
                }
                aviso.put("portada", segura);
            }
            avisos.add(aviso);
            if (avisos.size() >= 50) break;
        }
        return Map.of("total", avisos.size(), "avisos", avisos);
    }
}
