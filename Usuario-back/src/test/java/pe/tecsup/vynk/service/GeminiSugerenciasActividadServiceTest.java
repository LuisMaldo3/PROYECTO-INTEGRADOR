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

/** Tarea 2 (Mishel): Gemini sugiere a partir del contexto; nunca decide ni inventa. */
class GeminiSugerenciasActividadServiceTest {
    private HttpServer servidor;
    private final String baseAnterior = GeminiCliente.base;
    private final Map<String, Object> contexto = Map.of("nombreActividad", "Taller de robótica",
            "participacion", Map.of("estudiantesInscritos", 12), "comentarios", List.of("Faltó tiempo para terminar"));

    private GeminiSugerenciasActividadService servicio(String clave) {
        GeminiSugerenciasActividadService s = new GeminiSugerenciasActividadService(new ObjectMapper());
        ReflectionTestUtils.setField(s, "clave", clave);
        ReflectionTestUtils.setField(s, "modelo", "gemini-2.5-flash");
        return s;
    }

    private void google(int codigo, String textoGenerado) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/models/", ex -> {
            String cuerpo = codigo == 200
                    ? new ObjectMapper().writeValueAsString(Map.of("candidates",
                        List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", textoGenerado)))))))
                    : "{}";
            byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(codigo, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        servidor.start();
        GeminiCliente.base = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/models/";
    }

    @AfterEach void cerrar() {
        GeminiCliente.base = baseAnterior;
        if (servidor != null) servidor.stop(0);
    }

    @Test void sinClaveDiceDondeConfigurarlaYNoLlamaAGoogle() {
        var r = servicio("").generar(contexto);
        assertEquals("no_configurada", r.origen());
        assertTrue(r.aviso().contains("Usuario-back/.env"));
        assertEquals(List.of(), r.sugerencias());
    }

    @Test void conUnaRespuestaValidaDevuelveLasSugerencias() throws Exception {
        google(200, "{\"resumen\":\"Mejorar el tiempo asignado.\",\"sugerencias\":["
                + "{\"titulo\":\"Ampliar el horario\",\"detalle\":\"Varios estudiantes mencionaron que faltó tiempo.\"},"
                + "{\"titulo\":\"Reforzar la guía inicial\",\"detalle\":\"Ayudaría a que empiecen más rápido.\"}]}");
        var r = servicio("clave").generar(contexto);
        assertEquals("gemini", r.origen());
        assertEquals("Mejorar el tiempo asignado.", r.resumen());
        assertEquals(2, r.sugerencias().size());
        assertEquals("Ampliar el horario", r.sugerencias().get(0).get("titulo"));
    }

    @Test void unaSugerenciaSinTituloODetalleSeDescartaSinRomperLasDemas() throws Exception {
        google(200, "{\"resumen\":\"x\",\"sugerencias\":["
                + "{\"titulo\":\"\",\"detalle\":\"sin título\"},"
                + "{\"titulo\":\"Válida\",\"detalle\":\"\"},"
                + "{\"titulo\":\"Esta sí sirve\",\"detalle\":\"Tiene ambos campos.\"}]}");
        var r = servicio("clave").generar(contexto);
        assertEquals(1, r.sugerencias().size());
        assertEquals("Esta sí sirve", r.sugerencias().get(0).get("titulo"));
    }

    @Test void nuncaDevuelveMasDeSeisSugerencias() throws Exception {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i > 0) lista.append(",");
            lista.append("{\"titulo\":\"Sugerencia ").append(i).append("\",\"detalle\":\"Detalle ").append(i).append("\"}");
        }
        google(200, "{\"resumen\":\"x\",\"sugerencias\":[" + lista + "]}");
        assertEquals(6, servicio("clave").generar(contexto).sugerencias().size());
    }

    @Test void siGoogleRechazaLaClaveSeExplicaLaCausaYNoHaySugerenciasInventadas() throws Exception {
        google(403, "");
        var r = servicio("clave").generar(contexto);
        assertEquals("no_disponible", r.origen());
        assertEquals(List.of(), r.sugerencias());
        assertTrue(r.aviso().toLowerCase().contains("clave") || r.aviso().toLowerCase().contains("rechaz"));
    }

    @Test void siElModeloYaNoExisteSeUsaElDeRespaldoYSeLlegaAUnaRespuesta() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> llamadas = new java.util.ArrayList<>();
        servidor.createContext("/models/", ex -> {
            String modeloPedido = ex.getRequestURI().getPath().replace("/models/", "").replace(":generateContent", "");
            llamadas.add(modeloPedido);
            boolean esRespaldo = !modeloPedido.equals("gemini-2.5-flash");
            String cuerpo = esRespaldo
                    ? new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of("content",
                        Map.of("parts", List.of(Map.of("text", "{\"resumen\":\"ok\",\"sugerencias\":[{\"titulo\":\"A\",\"detalle\":\"B\"}]}")))))))
                    : "{}";
            byte[] b = cuerpo.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(esRespaldo ? 200 : 404, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        servidor.start();
        GeminiCliente.base = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/models/";

        var r = servicio("clave").generar(contexto);
        assertEquals("gemini", r.origen());
        assertEquals(1, r.sugerencias().size());
        assertTrue(llamadas.size() >= 2, "debió probar el modelo configurado y luego el de respaldo");
    }
}
