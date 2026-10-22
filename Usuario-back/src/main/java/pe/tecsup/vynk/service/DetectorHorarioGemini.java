package pe.tecsup.vynk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;

@Service
public class DetectorHorarioGemini {
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${GEMINI_API_KEY:}") private String clave;
    @Value("${GEMINI_MODEL:gemini-2.5-flash}") private String modelo;
    public record Lectura(List<BloqueOcupado> bloques, List<String> advertencias) {}
    public DetectorHorarioGemini(ObjectMapper json) { this.json = json; }
    public Lectura leer(byte[] imagen, String mime) {
        if (clave == null || clave.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configura GEMINI_API_KEY en el servidor para interpretar horarios.");
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(mime))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Selecciona una imagen JPG, PNG o WEBP.");
        if (imagen.length == 0 || imagen.length > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe pesar entre 1 byte y 5 MB.");
        String prompt = "Lee esta imagen de horario semanal del estudiante. Extrae SOLO intervalos de clase u otras "
          + "ocupaciones claramente visibles. Devuelve JSON exacto: {\"bloques\":[{\"dia\":1,\"inicio\":\"08:00\",\"fin\":\"10:00\",\"nombre\":\"Curso\"}],"
          + "\"advertencias\":[\"Texto poco legible\"]}. Dia numérico: lunes=1, martes=2, miercoles=3, jueves=4, viernes=5, sabado=6, domingo=7. "
          + "Usa formato 24 h HH:mm, divide bloques por día, no inventes datos ilegibles ni infieras horas. "
          + "Si no puedes leer un bloque, pon una advertencia en vez de inventarlo. No escribas datos personales.";
        String seguro = modelo.matches("[A-Za-z0-9._-]+") ? modelo : "gemini-2.5-flash";
        try {
            String payload = json.writeValueAsString(Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt), Map.of(
                    "inline_data", Map.of("mime_type", mime, "data", Base64.getEncoder().encodeToString(imagen)))))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0)));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + seguro + ":generateContent"))
                .header("Content-Type", "application/json").header("x-goog-api-key", clave)
                .timeout(Duration.ofSeconds(35)).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                System.err.println("Error de Gemini: HTTP " + resp.statusCode());
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini no pudo procesar la imagen."
                );
            }
            String texto = json.readTree(resp.body()).path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            JsonNode raiz = json.readTree(texto);
            List<BloqueOcupado> bloques = new ArrayList<>();
            List<String> avisos = new ArrayList<>();
            if (raiz.path("advertencias").isArray()) for (JsonNode n : raiz.path("advertencias")) {
                if (avisos.size() < 8) avisos.add(n.asText("").substring(0, Math.min(160, n.asText("").length())));
            }
            if (raiz.path("bloques").isArray()) for (JsonNode n : raiz.path("bloques")) {
                if (bloques.size() >= 100) break;
                try {
                    BloqueOcupado bloque = new BloqueOcupado(n.path("dia").asInt(0), n.path("inicio").asText(), n.path("fin").asText(), n.path("nombre").asText("Ocupado"));
                    bloques.addAll(DisponibilidadService.normalizar(List.of(bloque)));
                } catch (IllegalArgumentException ex) { avisos.add("Se descartó un bloque ambiguo: corrige la lectura antes de guardar."); }
            }
            if (bloques.isEmpty()) avisos.add("No se reconocieron bloques confiables; comprueba la imagen o añade tus horas manualmente.");
            return new Lectura(DisponibilidadService.normalizar(bloques), avisos);
        } catch (ResponseStatusException e) { throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "La lectura de imagen fue interrumpida.");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo interpretar la imagen. Inténtalo otra vez.");
        }
    }
}
