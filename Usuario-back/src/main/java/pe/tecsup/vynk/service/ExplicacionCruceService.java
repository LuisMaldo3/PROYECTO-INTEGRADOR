package pe.tecsup.vynk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExplicacionCruceService {
    private final ObjectMapper mapper;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    @Value("${GEMINI_API_KEY:}") private String clave;
    @Value("${GEMINI_MODEL:gemini-2.5-flash}") private String modelo;
    public ExplicacionCruceService(ObjectMapper mapper) { this.mapper = mapper; }
    public record Explicacion(String texto, String origen) {}
    public Explicacion explicar(CruceHorarioService.Cruce cruce) {
        String respaldo = "Tus actividades " + cruce.actividadA() + " y " + cruce.actividadB()
            + " coinciden el " + cruce.fecha() + " entre " + cruce.desde() + " y " + cruce.hasta()
            + " (" + cruce.minutos() + " minutos). Revisa tus horarios antes de asistir.";
        if (clave == null || clave.isBlank()) return new Explicacion(respaldo, "automatico");
        try {
            String prompt = "Explica en español, en máximo 60 palabras y tono claro, este conflicto de agenda. "
                + "No inventes horarios ni prometas alternativas: " + respaldo;
            String cuerpo = mapper.writeValueAsString(Map.of("contents", new Object[]{
                Map.of("parts", new Object[]{Map.of("text", prompt)})}));
            String nombreModelo = modelo.matches("[A-Za-z0-9._-]+") ? modelo : "gemini-2.5-flash";
            HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + nombreModelo + ":generateContent"))
                .header("x-goog-api-key", clave).header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(8)).POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
            HttpResponse<String> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) return new Explicacion(respaldo, "automatico");
            JsonNode partes = mapper.readTree(respuesta.body()).path("candidates").path(0).path("content").path("parts");
            String texto = partes.path(0).path("text").asText("").trim();
            return texto.isBlank() ? new Explicacion(respaldo, "automatico") : new Explicacion(texto, "ia");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Explicacion(respaldo, "automatico");
        }
    }
}
