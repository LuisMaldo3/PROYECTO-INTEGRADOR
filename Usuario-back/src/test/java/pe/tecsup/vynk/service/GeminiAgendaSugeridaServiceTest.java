package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** La agenda sugerida dice por qué no usó la IA, en vez de caer en silencio al cálculo sin IA. */
class GeminiAgendaSugeridaServiceTest {
    private HttpServer servidor;
    private final String baseAnterior = GeminiCliente.base;
    private final List<Map<String, Object>> candidatas = List.of(
            Map.of("id", "a1", "nombre", "Fútbol", "categoria", "Deportes", "fecha", "2026-11-17", "horaInicio", "10:00", "horaFin", "12:00"),
            Map.of("id", "a2", "nombre", "Karaoke", "categoria", "Música y canto", "fecha", "2026-11-17", "horaInicio", "15:00", "horaFin", "16:00"));

    private GeminiAgendaSugeridaService servicio(String clave) {
        GeminiAgendaSugeridaService s = new GeminiAgendaSugeridaService(new ObjectMapper());
        ReflectionTestUtils.setField(s, "clave", clave);
        ReflectionTestUtils.setField(s, "modelo", "gemini-2.5-flash");
        return s;
    }

    private void google(int codigo, String texto) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/models/", ex -> {
            String cuerpo = codigo == 200 ? new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", texto))))))) : "{}";
            byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, b.length); ex.getResponseBody().write(b); ex.close();
        });
        servidor.start();
        GeminiCliente.base = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/models/";
    }

    @AfterEach void cerrar() { GeminiCliente.base = baseAnterior; if (servidor != null) servidor.stop(0); }

    @Test void sinClaveDiceDondeConfigurarla() {
        var r = servicio("").elegirConEstado(List.of("deportes"), candidatas, List.of());
        assertEquals("sin_clave", r.estado());
        assertTrue(r.mensaje().contains("Usuario-back/.env"));
    }

    @Test void siGoogleRechazaLaClaveSeDiceLaCausa() throws Exception {
        google(403, "");
        var r = servicio("clave").elegirConEstado(List.of(), candidatas, List.of());
        assertEquals("error", r.estado());
        assertTrue(r.mensaje().contains("clave"));
    }

    @Test void conRespuestaValidaEligeSoloActividadesReales() throws Exception {
        google(200, "{\"actividades\":[{\"id\":\"a2\",\"motivo\":\"Te gusta la música\"},{\"id\":\"inventada\",\"motivo\":\"x\"}]}");
        var r = servicio("clave").elegirConEstado(List.of("musica"), candidatas, List.of());
        assertEquals("ok", r.estado());
        assertEquals(List.of("a2"), r.elecciones().stream().map(GeminiAgendaSugeridaService.Eleccion::id).toList());
    }

    @Test void siLaIaNoEligeNadaValidoSeAvisa() throws Exception {
        google(200, "{\"actividades\":[{\"id\":\"inventada\",\"motivo\":\"x\"}]}");
        assertEquals("respuesta_invalida", servicio("clave").elegirConEstado(List.of(), candidatas, List.of()).estado());
    }
}
