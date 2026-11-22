package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotivosHorarioServiceTest {
    @Test void explicaInteresCoincidenteYFranjaReal() {
        String texto = MotivosHorarioService.explicacion(
                Map.of("nombre", "Taller de programación", "descripcion", "Java"),
                List.of("programación"), LocalDate.parse("2026-11-02"),
                LocalTime.parse("10:00"), LocalTime.parse("13:00"));
        assertTrue(texto.contains("programación"));
        assertTrue(texto.contains("10:00"));
        assertTrue(texto.contains("13:00"));
    }

    @Test void nuncaInventaInteresSiNoExisteCoincidencia() {
        String texto = MotivosHorarioService.explicacion(Map.of("nombre", "Taller de música"),
                List.of("robótica"), LocalDate.parse("2026-11-03"),
                LocalTime.parse("15:00"), LocalTime.parse("17:00"));
        assertFalse(texto.contains("robótica"));
        assertTrue(texto.contains("actividad publicada disponible"));
    }

    @Test void nombraElInteresComoLoDiriaElEstudianteYLaFechaLegible() {
        String texto = MotivosHorarioService.explicacion(Map.of("nombre", "Taller de IA generativa"),
                List.of("ia"), LocalDate.parse("2026-11-23"), LocalTime.parse("13:00"), LocalTime.parse("14:00"));
        assertEquals("Coincide con tu interés en inteligencia artificial y cabe en tu horario libre del lunes 23 de noviembre"
                + " entre 13:00 y 14:00.", texto);
        String industria = MotivosHorarioService.explicacion(Map.of("nombre", "Charla de industria"),
                List.of("ia"), LocalDate.parse("2026-11-23"), LocalTime.parse("13:00"), LocalTime.parse("14:00"));
        assertTrue(industria.startsWith("Es una actividad publicada disponible"), "«ia» no es «industria»");
    }
}
