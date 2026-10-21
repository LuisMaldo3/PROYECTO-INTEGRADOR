package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConsultasNaturalesTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T16:00:00Z"), ZoneId.of("America/Lima"));

    @Test void extraeDeportesYAsumeTardeIndicandolo() {
        var q = ConsultaTemaHorario.interpretar("¿Qué deportes hay después de las 3?", RELOJ);
        assertEquals("deportes", q.tema());
        assertEquals(LocalTime.of(15, 0), q.hora());
        assertEquals(ConsultaTemaHorario.Modo.DESPUES, q.modo());
        assertTrue(q.tardeInferida());
    }

    @Test void entiendeTemaFechaYHoraExplicitamente() {
        var q = ConsultaTemaHorario.interpretar("¿Hay talleres de programación mañana a las 4 p. m.?", RELOJ);
        assertEquals("tecnologia", q.tema());
        assertEquals(LocalDate.of(2026, 10, 25), q.fecha());
        assertEquals(LocalTime.of(16, 0), q.hora());
    }

    @Test void aplicaAmbosFiltrosYCuposAlMismoEvento() {
        var futbol = actividad("Fútbol Tecsup", "deportes", "2026-10-25", "15:30", 5);
        var temprano = actividad("Vóley", "deportes", "2026-10-25", "10:00", 3);
        var taller = actividad("Programación", "tecnologia", "2026-10-25", "16:00", 6);
        var lleno = actividad("Gymkana", "deportes", "2026-10-25", "17:00", 0);
        var q = ConsultaTemaHorario.interpretar("¿Qué deportes hay después de las 3?", RELOJ);
        assertEquals(List.of(futbol), BusquedaTemaHorario.buscar(List.of(futbol, temprano, taller, lleno), q, RELOJ));
    }

    @Test void avisaCuandoNoHayCoincidencias() {
        var consulta = ConsultaTemaHorario.interpretar("¿Qué deportes hay después de las 3?", RELOJ);
        var respuesta = RespuestaSinResultados.para(consulta);
        assertTrue(respuesta.respuesta().contains("No encontré"));
        assertTrue(respuesta.actividades().isEmpty());
        assertEquals("tema_horario", respuesta.tipoConsulta());
    }

    @Test void lasHorasAmbiguasSolicitanPrecisionCuandoNoHayContexto() {
        var q = ConsultaTemaHorario.interpretar("¿Qué actividades de música hay a las 3?", RELOJ);
        assertFalse(q.aclaracion().isBlank());
    }

    private static Map<String,Object> actividad(String nombre, String categoria, String fecha, String inicio, int cupos) {
        return Map.of("nombre", nombre, "categoriaId", categoria, "fecha", fecha, "horaInicio", inicio,
                "horaFin", "18:00", "estado", "programada", "cuposDisponibles", cupos);
    }
}
