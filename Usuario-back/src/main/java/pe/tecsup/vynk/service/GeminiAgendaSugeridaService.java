package pe.tecsup.vynk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** La IA únicamente devuelve IDs del catálogo y motivos; jamás decide las horas reales. */
@Service
public class GeminiAgendaSugeridaService {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${GEMINI_API_KEY:}") private String clave;
    @Value("${GEMINI_MODEL:gemini-2.5-flash}") private String modelo;

    public record Eleccion(String id, String motivo) {}
    public GeminiAgendaSugeridaService(ObjectMapper json) { this.json = json; }

    public List<Eleccion> elegir(List<String> intereses, List<Map<String, Object>> candidatas,
                                List<Map<String, String>> ocupadas) {
        if (clave == null || clave.isBlank() || candidatas.isEmpty()) return List.of();
        try {
            List<Map<String, String>> opciones = candidatas.stream().limit(35).map(a -> Map.of(
                    "id", String.valueOf(a.get("id")), "nombre", String.valueOf(a.getOrDefault("nombre", "")),
                    "descripcion", String.valueOf(a.getOrDefault("descripcion", "")),
                    "categoria", String.valueOf(a.getOrDefault("categoriaId", "")),
                    "fecha", String.valueOf(a.get("fecha")),
                    "inicio", String.valueOf(a.get("horaInicio")), "fin", String.valueOf(a.get("horaFin"))
            )).toList();
            String prompt = "Propón una agenda de máximo 6 actividades para un estudiante. "
                    + "Respeta los horarios ocupados, nunca selecciones actividades que coincidan temporalmente entre sí "
                    + "ni con la agenda actual. Prioriza los intereses del alumno. Intereses: "
                    + json.writeValueAsString(intereses) + ". Inscripciones con horarios ocupados: "
                    + json.writeValueAsString(ocupadas) + ". Únicas candidatas permitidas: "
                    + json.writeValueAsString(opciones) + ". Responde SOLO JSON "
                    + "{\"actividades\":[{\"id\":\"id existente\",\"motivo\":\"motivo breve\"}]}. "
                    + "No inventes IDs, actividades, fechas ni horarios.";
            String payload = json.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.15)));
            String seguro = modelo.matches("[A-Za-z0-9._-]+") ? modelo : "gemini-2.5-flash";
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/models/" + seguro + ":generateContent"))
                    .timeout(Duration.ofSeconds(17)).header("Content-Type", "application/json")
                    .header("x-goog-api-key", clave).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) return List.of();
            String texto = json.readTree(respuesta.body()).path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText("");
            JsonNode seleccion = json.readTree(texto).path("actividades");
            Set<String> permitidos = new HashSet<>();
            opciones.forEach(a -> permitidos.add(a.get("id")));
            Set<String> vistos = new HashSet<>();
            List<Eleccion> resultado = new ArrayList<>();
            if (seleccion.isArray()) for (JsonNode item : seleccion) {
                String id = item.path("id").asText("");
                if (permitidos.contains(id) && vistos.add(id) && resultado.size() < 6) {
                    String motivo = item.path("motivo").asText("").strip();
                    resultado.add(new Eleccion(id, motivo.length() > 250 ? motivo.substring(0, 250) : motivo));
                }
            }
            return resultado;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) { return List.of(); }
    }
}
