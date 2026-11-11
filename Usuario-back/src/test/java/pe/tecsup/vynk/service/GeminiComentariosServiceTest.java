package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.ComentarioActividad;

class GeminiComentariosServiceTest {
    final ObjectMapper json = new ObjectMapper();
    final GeminiComentariosService servicio = new GeminiComentariosService(json);

    @Test void sinComentariosNoSolicitaIaNiInventaResumen() {
        var d = servicio.analizar(List.of());
        assertEquals("sin_comentarios", d.origen());
        assertEquals("", d.resumen());
        assertTrue(d.temas().isEmpty());
    }

    @Test void limpiaCorreosTelefonosYUrlsAntesDeEnviarDatos() {
        String limpio = GeminiComentariosService.anonimizar(
                "Contacta a ana@tecsup.edu.pe por https://ejemplo.pe o al 987654321");
        assertFalse(limpio.contains("ana@"));
        assertFalse(limpio.contains("https://"));
        assertFalse(limpio.contains("987654321"));
    }

    @Test void contarTemasPorComentariosUnicosYNoPorNumeroAfirmado() throws Exception {
        var d = json.readTree("[{\"tema\":\"Horarios\",\"cantidad\":999,\"comentarioIds\":[1,1,2,900]},"
                + "{\"tema\":\"Sin repetir\",\"comentarioIds\":[1]}]");
        var temas = GeminiComentariosService.validarTemas(d, 2);
        assertEquals(1, temas.size());
        assertEquals("Horarios", temas.get(0).get("tema"));
        assertEquals(2, temas.get(0).get("cantidad"));
    }
}
