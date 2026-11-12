package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La IA: modelo de respaldo y mensajes con la causa real, contra un servidor local que imita a Google. */
class GeminiClienteTest {
    private HttpServer servidor;
    private final List<String> pedidos = new ArrayList<>();
    private final List<String> claves = new ArrayList<>();
    private String baseAnterior;

    /** Responde a cada modelo con el código indicado (404 si no figura). */
    private void levantar(Map<String, Integer> respuestas) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/models/", ex -> {
            String modelo = ex.getRequestURI().getPath().replace("/models/", "").replace(":generateContent", "");
            pedidos.add(modelo);
            claves.add(ex.getRequestHeaders().getFirst("x-goog-api-key"));
            int codigo = respuestas.getOrDefault(modelo, 404);
            byte[] cuerpo = (codigo == 200 ? "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}" : "{\"error\":\"x\"}").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, cuerpo.length);
            ex.getResponseBody().write(cuerpo);
            ex.close();
        });
        servidor.start();
        GeminiCliente.base = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/models/";
    }

    @BeforeEach
    void guardarBase() { baseAnterior = GeminiCliente.base; }

    @AfterEach
    void restaurar() {
        GeminiCliente.base = baseAnterior;
        if (servidor != null) servidor.stop(0);
    }

    private HttpResponse<String> enviar(String modelo) throws Exception {
        return GeminiCliente.enviar(HttpClient.newHttpClient(), modelo, "clave-secreta", "{}", Duration.ofSeconds(5));
    }

    @Test
    void usaElModeloConfiguradoSiExiste() throws Exception {
        levantar(Map.of("gemini-2.5-flash", 200));
        assertEquals(200, enviar("gemini-2.5-flash").statusCode());
        assertEquals(List.of("gemini-2.5-flash"), pedidos);
        assertEquals("clave-secreta", claves.get(0), "la clave viaja en la cabecera, no en la dirección");
    }

    @Test
    void siElModeloYaNoExistePruebaElDeRespaldo() throws Exception {
        levantar(Map.of("gemini-3.8-flash", 200));
        assertEquals(200, enviar("gemini-2.5-flash").statusCode());
        assertEquals(List.of("gemini-2.5-flash", "gemini-3.8-flash"), pedidos);
    }

    @Test
    void siNingunoExisteDevuelveElUltimo404() throws Exception {
        levantar(Map.of());
        assertEquals(404, enviar("gemini-viejo").statusCode());
        assertEquals(3, pedidos.size(), "el configurado y los dos de respaldo");
    }

    @Test
    void unaClaveMalaOUnLimiteDeUsoNoSeReintentanConOtroModelo() throws Exception {
        for (int codigo : new int[] {400, 401, 403, 429, 500}) {
            pedidos.clear();
            levantar(Map.of("gemini-2.5-flash", codigo));
            assertEquals(codigo, enviar("gemini-2.5-flash").statusCode());
            assertEquals(1, pedidos.size(), "código " + codigo);
            servidor.stop(0);
        }
    }

    @Test
    void losMensajesDicenLaCausaYNuncaLlevanLaClave() {
        assertTrue(GeminiCliente.mensaje(401).contains("clave"));
        assertTrue(GeminiCliente.mensaje(429).contains("límite"));
        assertTrue(GeminiCliente.mensaje(404).contains("GEMINI_MODEL"));
        assertTrue(GeminiCliente.mensaje(500).contains("500"));
        for (int c : new int[] {400, 401, 403, 404, 429, 500}) assertFalse(GeminiCliente.mensaje(c).contains("clave-secreta"));
    }

    @Test
    void conLaRespuestaDeGoogleSeExplicaLaCausaReal() {
        assertTrue(GeminiCliente.mensaje(400, "{\"error\":{\"message\":\"API key expired. Please renew the API key.\"}}").contains("venció"));
        assertTrue(GeminiCliente.mensaje(403, "{\"error\":{\"message\":\"Generative Language API has not been used in project 1 before or it is disabled\"}}").contains("desactivada"));
        assertTrue(GeminiCliente.mensaje(403, "{\"error\":{\"message\":\"Requests from this Android client application <empty> are blocked.\"}}").contains("restricciones"));
        assertTrue(GeminiCliente.mensaje(401, "").contains("rechazó la clave"));
        assertTrue(GeminiCliente.mensaje(429, "").contains("límite"));
    }
}
