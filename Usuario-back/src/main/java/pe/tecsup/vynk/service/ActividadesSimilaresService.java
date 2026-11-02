package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/** US-22-T01/T02: recomendaciones explicables, sin IA ni datos ficticios.
 * Solo se sugieren otras actividades PROGRAMADAS (nunca canceladas/finalizadas).
 */
@Service
public class ActividadesSimilaresService {
    private final MongoTemplate mongo;

    public ActividadesSimilaresService(MongoTemplate mongo) { this.mongo = mongo; }

    public List<Document> buscar(Document origen, int limite) {
        if (origen == null || limite <= 0) return List.of();
        List<Document> publicados = mongo.find(
                new Query(Criteria.where("estado").is("programada")), Document.class, "actividades");
        String idOriginal = String.valueOf(origen.get("_id"));
        List<Document> candidatos = new ArrayList<>();
        for (Document documento : publicados) {
            if (idOriginal.equals(String.valueOf(documento.get("_id")))) continue;
            if (puntuacion(origen, documento) > 0) candidatos.add(documento);
        }
        candidatos.sort(Comparator
                .comparingInt((Document d) -> puntuacion(origen, d)).reversed()
                .thenComparing(d -> String.valueOf(d.getOrDefault("fecha", "")))
                .thenComparing(d -> String.valueOf(d.getOrDefault("nombre", ""))));
        return candidatos.subList(0, Math.min(limite, candidatos.size()));
    }

    /** Priorizar área/categoría/modalidad y coincidencias temáticas en el título. */
    public static int puntuacion(Map<?, ?> original, Map<?, ?> alternativa) {
        int puntos = 0;
        for (String campo : List.of("areaTecsup", "categoriaId")) {
            Object a = original.get(campo), b = alternativa.get(campo);
            if (a != null && b != null && !String.valueOf(a).isBlank()
                    && !"null".equalsIgnoreCase(String.valueOf(a))
                    && String.valueOf(a).equalsIgnoreCase(String.valueOf(b))) puntos += campo.equals("areaTecsup") ? 5 : 4;
        }
        if ("competencia".equals(original.get("tipoActividad"))
                && original.get("tipoActividad").equals(alternativa.get("tipoActividad"))) puntos += 2;
        if (original.get("modalidad") != null && original.get("modalidad").equals(alternativa.get("modalidad"))) puntos++;
        Set<String> palabras = palabras(original.get("nombre"));
        palabras.retainAll(palabras(alternativa.get("nombre")));
        puntos += Math.min(4, palabras.size()) * 2;
        return puntos;
    }

    private static Set<String> palabras(Object valor) {
        String normalizado = Normalizer.normalize(String.valueOf(valor == null ? "" : valor), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        Set<String> stop = Set.of("de", "la", "el", "del", "en", "para", "y", "un", "una", "los", "las", "torneo", "competencia", "actividad");
        Set<String> result = new HashSet<>(Arrays.asList(normalizado.split("[^a-z0-9]+")));
        result.removeIf(p -> p.length() < 4 || stop.contains(p));
        return result;
    }
}
