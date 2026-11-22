package pe.tecsup.vynk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Envía avisos con la API HTTP v1 de Firebase Cloud Messaging usando una cuenta de servicio. */
public class NotificadorFcm implements NotificadorPush {
    private static final Logger log = LoggerFactory.getLogger(NotificadorFcm.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String ALCANCE = "https://www.googleapis.com/auth/firebase.messaging";
    static final String URL_FCM = "https://fcm.googleapis.com";
    /** FCM no guarda un aviso más de 28 días. */
    private static final Duration VIDA_MAXIMA = Duration.ofDays(28);

    private final Supplier<String> tokenDeAcceso;
    private final URI destino;
    private final HttpClient http;

    public NotificadorFcm(Supplier<String> tokenDeAcceso, String proyecto, String urlBase, HttpClient http) {
        this.tokenDeAcceso = tokenDeAcceso;
        this.destino = URI.create(urlBase + "/v1/projects/" + proyecto + "/messages:send");
        this.http = http;
    }

    /** Lee el JSON de la cuenta de servicio de Firebase (nunca se sube a Git; se indica con FCM_CREDENCIALES). */
    public static NotificadorFcm desdeCredenciales(Path archivo, String proyectoIndicado) throws IOException {
        GoogleCredentials credenciales;
        try (InputStream in = Files.newInputStream(archivo)) {
            credenciales = GoogleCredentials.fromStream(in).createScoped(ALCANCE);
        }
        String proyecto = proyectoIndicado;
        if ((proyecto == null || proyecto.isBlank()) && credenciales instanceof ServiceAccountCredentials cuenta) {
            proyecto = cuenta.getProjectId();
        }
        if (proyecto == null || proyecto.isBlank()) {
            throw new IOException("No se pudo saber el proyecto de Firebase: define FCM_PROYECTO.");
        }
        GoogleCredentials usadas = credenciales;
        Supplier<String> acceso = () -> {
            try {
                usadas.refreshIfExpired();
                return usadas.getAccessToken().getTokenValue();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return new NotificadorFcm(acceso, proyecto.strip(), URL_FCM,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    @Override public boolean disponible() { return true; }

    @Override
    public Resultado enviar(String tokenDispositivo, Map<String, String> datos, Duration vida) {
        long segundos = Math.max(60, Math.min(vida == null ? 1800 : vida.toSeconds(), VIDA_MAXIMA.toSeconds()));
        try {
            String cuerpo = JSON.writeValueAsString(Map.of("message", Map.of(
                    "token", tokenDispositivo,
                    "data", datos,
                    "android", Map.of("priority", "HIGH", "ttl", segundos + "s"))));
            HttpRequest peticion = HttpRequest.newBuilder(destino)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + tokenDeAcceso.get())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            return interpretar(respuesta.statusCode(), respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.ERROR_TEMPORAL;
        } catch (JsonProcessingException e) {
            log.error("No se pudo armar el aviso de FCM", e);
            return Resultado.ERROR_PERMANENTE;
        } catch (IOException | RuntimeException e) { // incluye UncheckedIOException del token de acceso
            // Sin red o sin poder obtener el token de acceso: se reintenta en el siguiente ciclo.
            log.warn("No se pudo contactar a FCM: {}", e.toString());
            return Resultado.ERROR_TEMPORAL;
        }
    }

    /** Traduce la respuesta HTTP de FCM. Se deja visible para probarla sin red. */
    static Resultado interpretar(int estado, String cuerpo) {
        String texto = cuerpo == null ? "" : cuerpo;
        if (estado >= 200 && estado < 300) return Resultado.ENVIADO;
        // UNREGISTERED llega como 404; un token mal formado, como 400 que menciona el campo message.token.
        if (estado == 404 || (estado == 400 && (texto.contains("message.token") || texto.contains("UNREGISTERED"))))
            return Resultado.TOKEN_INVALIDO;
        if (estado == 401 || estado == 403) {
            log.error("FCM rechazó las credenciales del servidor (HTTP {}). Revisa FCM_CREDENCIALES.", estado);
            return Resultado.ERROR_TEMPORAL;
        }
        if (estado == 429 || estado >= 500) return Resultado.ERROR_TEMPORAL;
        log.error("FCM rechazó el aviso (HTTP {}): {}", estado, texto.length() > 300 ? texto.substring(0, 300) : texto);
        return Resultado.ERROR_PERMANENTE;
    }
}
