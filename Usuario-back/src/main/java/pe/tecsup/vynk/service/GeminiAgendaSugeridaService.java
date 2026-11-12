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
    /** Lo que eligió la IA y, si no pudo, por qué (estado: ok, sin_clave, error, sin_conexion, respuesta_invalida). */
    public record Resultado(List<Eleccion> elecciones, String estado, String mensaje) {
        public static Resultado fallo(String estado, String mensaje) { return new Resultado(List.of(), estado, mensaje); }
    }
    public GeminiAgendaSugeridaService(ObjectMapper json) { this.json = json; }

    public List<Eleccion> elegir(List<String> intereses, List<Map<String, Object>> candidatas,
                                List<Map<String, String>> ocupadas) {
        return elegirConEstado(intereses, candidatas, ocupadas).elecciones();
    }

    public Resultado elegirConEstado(List<String> intereses, List<Map<String, Object>> candidatas,
                                     List<Map<String, String>> ocupadas) {
        if (candidatas.isEmpty()) return Resultado.fallo("sin_candidatas", "No hay actividades disponibles para proponer.");
        if (clave == null || clave.isBlank())
            return Resultado.fallo("sin_clave", "falta configurar la clave de Gemini (GEMINI_API_KEY) en Usuario-back/.env");
        try {
            List<Map<String, String>> opciones = candidatas.stream().limit(35).map(a -> Map.of(
                    "id", String.valueOf(a.get("id")), "nombre", String.valueOf(a.getOrDefault("nombre", "")),
                    "descripcion", String.valueOf(a.getOrDefault("descripcion", "")),
                    "categoria", String.valueOf(a.getOrDefault("categoria", a.getOrDefault("categoriaId", ""))),
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
            HttpResponse<String> respuesta = GeminiCliente.enviar(http, seguro, clave, payload, Duration.ofSeconds(17));
            if (respuesta.statusCode() / 100 != 2) return Resultado.fallo("error", GeminiCliente.mensaje(respuesta.statusCode(), respuesta.body()));
            String texto = json.readTree(respuesta.body()).path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text").asText("");
            JsonNode seleccion;
            try { seleccion = json.readTree(texto).path("actividades"); }
            catch (Exception e) { return Resultado.fallo("respuesta_invalida", "la IA respondió algo que no se pudo leer"); }
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
            if (resultado.isEmpty()) return Resultado.fallo("respuesta_invalida", "la IA no eligió actividades válidas");
            return new Resultado(resultado, "ok", "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.fallo("sin_conexion", "se interrumpió la consulta a Google");
        } catch (java.io.IOException e) {
            return Resultado.fallo("sin_conexion", "no se pudo conectar con Google (revisa la conexión a internet del servidor)");
        } catch (Exception e) { return Resultado.fallo("error", "la IA no pudo responder"); }
    }
}
