package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.service.NotificadorPush.Resultado;

class NotificadorFcmTest {
    HttpServer servidor;
    final AtomicReference<String> ruta = new AtomicReference<>();
    final AtomicReference<String> autorizacion = new AtomicReference<>();
    final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    volatile int estado = 200;
    volatile String respuesta = "{\"name\":\"projects/vynk-e8ac4/messages/1\"}";
    NotificadorFcm fcm;

    @BeforeEach void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            ruta.set(intercambio.getRequestURI().getPath());
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        fcm = new NotificadorFcm(() -> "acceso-123", "vynk-e8ac4",
                "http://127.0.0.1:" + servidor.getAddress().getPort(), HttpClient.newHttpClient());
    }

    @AfterEach void detener() { servidor.stop(0); }

    @Test void enviaElAvisoComoDatosDeAltaPrioridadAlProyectoCorrecto() throws Exception {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("tipo", "recordatorio_actividad");
        datos.put("actividadId", "act1");

        assertEquals(Resultado.ENVIADO, fcm.enviar("token-del-celular-0123456789", datos));

        assertEquals("/v1/projects/vynk-e8ac4/messages:send", ruta.get());
        assertEquals("Bearer acceso-123", autorizacion.get());
        JsonNode mensaje = new ObjectMapper().readTree(cuerpoRecibido.get()).get("message");
        assertEquals("token-del-celular-0123456789", mensaje.get("token").asText());
        assertEquals("act1", mensaje.get("data").get("actividadId").asText());
        assertEquals("recordatorio_actividad", mensaje.get("data").get("tipo").asText());
        assertEquals("HIGH", mensaje.get("android").get("priority").asText());
        assertEquals("1800s", mensaje.get("android").get("ttl").asText());
        assertNull(mensaje.get("notification"), "va solo como datos: la app decide cómo mostrarlo");
    }

    @Test void unCelularQueYaNoExisteSeReportaComoTokenInvalido() {
        estado = 404;
        respuesta = "{\"error\":{\"code\":404,\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}";
        assertEquals(Resultado.TOKEN_INVALIDO, fcm.enviar("token-del-celular-0123456789", Map.of()));
    }

    @Test void unTokenMalFormadoSeReportaComoTokenInvalido() {
        estado = 400;
        respuesta = "{\"error\":{\"status\":\"INVALID_ARGUMENT\",\"details\":[{\"field\":\"message.token\"}]}}";
        assertEquals(Resultado.TOKEN_INVALIDO, fcm.enviar("token-del-celular-0123456789", Map.of()));
    }

    @Test void erroresDelServicioSonTemporalesYUnAvisoRechazadoEsPermanente() {
        assertEquals(Resultado.ERROR_TEMPORAL, NotificadorFcm.interpretar(503, "{}"));
        assertEquals(Resultado.ERROR_TEMPORAL, NotificadorFcm.interpretar(429, "{}"));
        assertEquals(Resultado.ERROR_TEMPORAL, NotificadorFcm.interpretar(401, "{}"));
        assertEquals(Resultado.ERROR_TEMPORAL, NotificadorFcm.interpretar(403, "{}"));
        assertEquals(Resultado.ERROR_PERMANENTE, NotificadorFcm.interpretar(400, "{\"error\":\"payload\"}"));
        assertEquals(Resultado.ENVIADO, NotificadorFcm.interpretar(200, null));
    }

    @Test void sinRedElErrorEsTemporal() {
        servidor.stop(0);
        assertEquals(Resultado.ERROR_TEMPORAL, fcm.enviar("token-del-celular-0123456789", Map.of()));
    }

    @Test void siNoSePuedeObtenerElTokenDeAccesoElErrorEsTemporal() {
        NotificadorFcm sinAcceso = new NotificadorFcm(() -> { throw new java.io.UncheckedIOException(new IOException("sin red")); },
                "vynk-e8ac4", "http://127.0.0.1:" + servidor.getAddress().getPort(), HttpClient.newHttpClient());
        assertEquals(Resultado.ERROR_TEMPORAL, sinAcceso.enviar("token-del-celular-0123456789", Map.of()));
    }

    @Test void sinCredencialesElNotificadorDesactivadoNoEnvia() {
        var apagado = new NotificadorDesactivado();
        assertFalse(apagado.disponible());
        assertTrue(fcm.disponible());
    }
}
