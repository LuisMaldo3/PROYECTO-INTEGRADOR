package pe.tecsup.vynk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** US-60: Gemini resume exclusivamente cifras agregadas; nunca recibe nombres, correos ni JWT. */
@Service
public class GeminiResumenInscripcionesService {
    public record Respuesta(String texto, String origen, String aviso) { }
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    @Value("${GEMINI_API_KEY:}") private String clave;
    @Value("${GEMINI_MODEL:gemini-2.5-flash}") private String modelo;

    public GeminiResumenInscripcionesService(ObjectMapper json) {
        this.json = json;
    }

    public Respuesta resumir(Map<String, Object> cifras) {
        if (clave == null || clave.isBlank()) {
            return new Respuesta("", "no_configurada", "Configura GEMINI_API_KEY en el backend para generar el resumen.");
        }
        // Lista blanca: no transmitir datos personales de estudiantes ni metadatos de sesión.
        Map<String, Object> agrupadas = new LinkedHashMap<>();
        for (String campo : List.of("unidadCupo", "estudiantesInscritos", "equiposRegistrados",
                "equiposConfirmados", "equiposPendientes", "reemplazosRegistrados", "cuposOcupados",
                "cuposDisponibles", "maximoParticipantes", "porcentajeParticipacion", "nivel")) {
            agrupadas.put(campo, cifras.get(campo));
        }
        try {
            String prompt = "Eres analista del evento académico T&C Week. Genera en español un resumen breve "
                    + "(máximo 130 palabras) sobre inscripciones, equipos y reemplazos. "
                    + "Basa cada afirmación exclusivamente en las cifras agregadas provistas. "
                    + "Distingue cupos de equipos de cupos de estudiantes; no confundas ocupación con asistencia. "
                    + "Si no hay capacidad máxima, no calcules un porcentaje. "
                    + "No inventes inscritos, causas de reemplazos, ganadores ni datos que faltan. "
                    + "Agrega una recomendación concreta de seguimiento. Datos: "
                    + json.writeValueAsString(agrupadas);
            Map<String, Object> generacion = new LinkedHashMap<>();
            generacion.put("temperature", 0.25);
            generacion.put("maxOutputTokens", 1024);
            String seguro = modelo != null && modelo.matches("[A-Za-z0-9._-]{1,70}") ? modelo : "gemini-2.5-flash";
            if (seguro.startsWith("gemini-2.5-flash")) generacion.put("thinkingConfig", Map.of("thinkingBudget", 0));
            String payload = json.writeValueAsString(Map.of("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))) , "generationConfig", generacion));
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/" + seguro + ":generateContent"))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", clave)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                String aviso = switch (respuesta.statusCode()) {
                    case 400 -> "Gemini rechazó la solicitud; revisa el modelo configurado.";
                    case 401, 403 -> "Gemini no autorizó la clave configurada.";
                    case 404 -> "El modelo Gemini configurado no está disponible para esta clave.";
                    case 429 -> "Gemini alcanzó el límite de solicitudes; intenta más tarde.";
                    default -> "Gemini no pudo generar el resumen (HTTP " + respuesta.statusCode() + ").";
                };
                return new Respuesta("", "no_disponible", aviso);
            }
            JsonNode parts = json.readTree(respuesta.body()).path("candidates").path(0)
                    .path("content").path("parts");
            StringBuilder texto = new StringBuilder();
            if (parts.isArray()) for (JsonNode part : parts) {
                String fragmento = part.path("text").asText("").trim();
                if (!fragmento.isBlank()) {
                    if (texto.length() > 0) texto.append("\n");
                    texto.append(fragmento);
                }
            }
            String contenido = texto.toString().trim();
            if (contenido.isBlank()) return new Respuesta("", "sin_respuesta",
                    "Gemini respondió sin texto. Intenta nuevamente.");
            return new Respuesta(contenido.length() > 3000 ? contenido.substring(0, 3000) : contenido,
                    "gemini", "Resumen generado a partir de datos actuales; verifica su interpretación.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Respuesta("", "no_disponible", "Se interrumpió la consulta a Gemini.");
        } catch (Exception ex) {
            // No registrar la clave ni el cuerpo de la solicitud.
            return new Respuesta("", "no_disponible", "No se pudo conectar con Gemini.");
        }
    }
}
