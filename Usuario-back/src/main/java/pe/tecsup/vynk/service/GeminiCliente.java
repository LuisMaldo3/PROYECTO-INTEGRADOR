package pe.tecsup.vynk.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Llamada única a Gemini para todo el backend. Si el modelo configurado ya no existe en Google (404) prueba los de
 * respaldo en vez de fallar: Google anunció que gemini-2.5-flash pasa a responder con gemini-3.8-flash desde el
 * 1 de diciembre de 2026. Cualquier otra respuesta (clave inválida, límite de uso, error) se devuelve tal cual
 * para que quien llama explique la causa real.
 */
public final class GeminiCliente {
    private GeminiCliente() {}

    static final List<String> RESPALDO = List.of("gemini-3.8-flash", "gemini-2.5-flash");
    /** Dirección base; solo se cambia en las pruebas. */
    static String base = "https://generativelanguage.googleapis.com/v1beta/models/";

    public static HttpResponse<String> enviar(HttpClient http, String modelo, String clave, String payload, Duration timeout)
            throws IOException, InterruptedException {
        List<String> modelos = new ArrayList<>();
        modelos.add(modelo);
        for (String m : RESPALDO) if (!modelos.contains(m)) modelos.add(m);
        HttpResponse<String> ultima = null;
        for (String m : modelos) {
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(base + m + ":generateContent"))
                    .header("Content-Type", "application/json").header("x-goog-api-key", clave)
                    .timeout(timeout).POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            ultima = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (ultima.statusCode() != 404) return ultima;
        }
        return ultima;
    }

    /** Qué pasó, en español, según el código HTTP que devolvió Google. Nunca incluye la clave. */
    public static String mensaje(int codigo) {
        return switch (codigo) {
            case 400, 401, 403 -> "Google rechazó la clave de la IA (GEMINI_API_KEY). Revisa que esté completa, activa y con permiso para Gemini.";
            case 404 -> "El modelo de IA configurado ya no existe en Google y ninguno de respaldo respondió. Cambia GEMINI_MODEL por uno vigente.";
            case 429 -> "Se alcanzó el límite de uso de la IA. Espera unos minutos o revisa tu cuota en Google AI Studio.";
            default -> "Google no pudo responder ahora (HTTP " + codigo + "). Inténtalo de nuevo en unos minutos.";
        };
    }

    /** Igual que {@link #mensaje(int)}, pero usa el motivo que Google explica en su respuesta (sin la clave). */
    public static String mensaje(int codigo, String cuerpo) {
        String t = cuerpo == null ? "" : cuerpo.toLowerCase();
        if (codigo == 429 || t.contains("resource_exhausted")) return mensaje(429);
        if (codigo == 404) return mensaje(404);
        if (t.contains("expired")) return "La clave de Gemini venció. Crea una nueva en Google AI Studio y reemplázala en Usuario-back/.env.";
        if (t.contains("service_disabled") || t.contains("has not been used") || t.contains(" is disabled"))
            return "La API de Gemini (Generative Language API) está desactivada en el proyecto de esa clave. Actívala o crea la clave desde Google AI Studio.";
        if (t.contains("blocked") || t.contains("referer") || t.contains("referrer") || t.contains("android") || t.contains("ip address"))
            return "La clave tiene restricciones que impiden usarla desde el servidor (por ejemplo, solo para la app Android). Usa una clave de Google AI Studio sin restricción de aplicación.";
        if (t.contains("location is not supported")) return "Google no ofrece la API de Gemini en la región desde donde sale la conexión del servidor.";
        return mensaje(codigo);
    }
}
