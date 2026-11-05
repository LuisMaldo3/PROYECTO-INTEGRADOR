package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.ConversacionAsistente;

class ContextoConversacionServiceTest {
    private ConversacionAsistente.Mensaje respuesta(String nombre) {
        return new ConversacionAsistente.Mensaje("asistente", "Actividad publicada", "catalogo", "", List.of(),
                "general", List.of(Map.of("id", "a1", "nombre", nombre)), List.of());
    }

    @Test void reutilizaLaActividadEnSeguimientoDeLugar() {
        var r = ContextoConversacionService.resolver("¿Y dónde será?", List.of(respuesta("Taller de robótica")));
        assertTrue(r.pregunta().contains("Taller de robótica"));
        assertTrue(r.pregunta().contains("Dónde"));
    }

    @Test void reutilizaLaActividadEnSeguimientoDeRequisitos() {
        var r = ContextoConversacionService.resolver("¿Y qué requisitos tiene?", List.of(respuesta("Charla de IA")));
        assertEquals("¿Qué requisitos tiene Charla de IA?", r.pregunta());
    }

    @Test void evitaInventarActividadCuandoLaRespuestaTrajoVarias() {
        var ambiguo = new ConversacionAsistente.Mensaje("asistente", "Dos actividades", "catalogo", "", List.of(),
                "general", List.of(Map.of("id", "1", "nombre", "Taller"), Map.of("id", "2", "nombre", "Feria")), List.of());
        assertTrue(ContextoConversacionService.resolver("¿Y dónde es?", List.of(ambiguo)).aclaracion().contains("varias actividades"));
    }

    @Test void nuevaPreguntaIndependienteNoArrastraContexto() {
        assertEquals("¿Qué actividades hay mañana?", ContextoConversacionService.resolver(
                "¿Qué actividades hay mañana?", List.of(respuesta("Robótica"))).pregunta());
    }

    @Test void sinHistorialNoInventaElNombre() {
        assertEquals("¿Y dónde será?", ContextoConversacionService.resolver("¿Y dónde será?", List.of()).pregunta());
    }
}
