package pe.tecsup.vynk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiRecomendacionesService {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    @Value("${GEMINI_API_KEY:}") private String clave;
    @Value("${GEMINI_MODEL:gemini-2.5-flash}") private String modelo;
    public GeminiRecomendacionesService(ObjectMapper json) { this.json = json; }
    public record Decision(String id, String motivo) {}
    public record Resultado(List<Decision> decisiones, String origen, String aviso) {}
    public Resultado ordenar(List<String> intereses, List<Map<String,Object>> candidatas) {
        if (candidatas.isEmpty()) return new Resultado(List.of(), "sin_actividades", "No hay actividades publicadas.");
        if (clave == null || clave.isBlank()) return new Resultado(List.of(), "ia_no_disponible", "Configura GEMINI_API_KEY en Spring Boot para activar Gemini.");
        try {
            List<Map<String,Object>> resumen = candidatas.stream().limit(35).map(a -> Map.<String,Object>of(
                "id", String.valueOf(a.get("id")), "nombre", String.valueOf(a.get("nombre")),
                "descripcion", String.valueOf(a.getOrDefault("descripcion", "")),
                "categoriaId", String.valueOf(a.getOrDefault("categoriaId", "")),
                "fecha", String.valueOf(a.getOrDefault("fecha", "")))).toList();
            String prompt = "Eres un recomendador de actividades de estudiantes. Intereses: " + json.writeValueAsString(intereses)
                + ". Actividades REALES permitidas: " + json.writeValueAsString(resumen)
                + ". Ordena las más relevantes según los intereses y justifica cada una brevemente en español. "
                + "No inventes actividades ni IDs. Responde SOLO JSON: {\"recomendaciones\":[{\"id\":\"id existente\",\"motivo\":\"justificación\"}]}. Máximo 12.";
            String payload = json.writeValueAsString(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.25)));
            String modeloSeguro = modelo.matches("[A-Za-z0-9._-]+") ? modelo : "gemini-2.5-flash";
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"+modeloSeguro+":generateContent"))
                .timeout(Duration.ofSeconds(18)).header("Content-Type","application/json").header("x-goog-api-key", clave)
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode()/100 != 2) return new Resultado(List.of(), "ia_no_disponible", "Gemini no respondió correctamente (HTTP " + resp.statusCode() + ").");
            String respuesta = json.readTree(resp.body()).path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            JsonNode lista = json.readTree(respuesta).path("recomendaciones");
            Set<String> validos = new HashSet<>(); for(var a:candidatas) validos.add(String.valueOf(a.get("id")));
            Set<String> vistos = new HashSet<>(); List<Decision> decisiones = new ArrayList<>();
            if (lista.isArray()) for(JsonNode item:lista) {
                String id = item.path("id").asText(""); String motivo = item.path("motivo").asText("").trim();
                if(validos.contains(id) && vistos.add(id) && !motivo.isBlank() && decisiones.size()<12) decisiones.add(new Decision(id,motivo));
            }
            if(decisiones.isEmpty()) return new Resultado(List.of(), "ia_no_disponible", "Gemini no devolvió recomendaciones válidas.");
            return new Resultado(decisiones, "ia", "Recomendaciones ordenadas y explicadas por Gemini.");
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); return new Resultado(List.of(),"ia_no_disponible","La consulta a Gemini fue interrumpida."); }
          catch (Exception ex) { return new Resultado(List.of(),"ia_no_disponible","No fue posible obtener recomendaciones de Gemini."); }
    }
}
