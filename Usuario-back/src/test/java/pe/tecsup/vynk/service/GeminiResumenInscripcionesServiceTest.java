package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiResumenInscripcionesServiceTest {
    @Test void noSimulaUnTextoIAcuandoNoHayClave() {
        var ia = new GeminiResumenInscripcionesService(new ObjectMapper());
        var respuesta = ia.resumir(Map.of("estudiantesInscritos", 4));
        assertEquals("no_configurada", respuesta.origen());
        assertEquals("", respuesta.texto());
        assertTrue(respuesta.aviso().contains("GEMINI_API_KEY"));
    }
}
