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

class ActividadesMomentoTest {
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneId.of("America/Lima"));

    @Test void reconoceDiaYHoraEnEspanol() {
        var consulta = ConsultaMomento.interpretar("¿Qué actividades hay mañana a las 3 p. m.?", RELOJ);
        assertEquals(LocalDate.of(2026, 10, 25), consulta.fecha());
        assertEquals(LocalTime.of(15, 0), consulta.hora());
        assertTrue(consulta.aclaracion().isBlank());
    }

    @Test void reconoceDiaDeSemanaYFechaExplicita() {
        assertEquals(LocalDate.of(2026, 10, 26), ConsultaMomento.interpretar("el lunes a las 15:30", RELOJ).fecha());
        assertEquals(LocalDate.of(2026, 11, 10), ConsultaMomento.interpretar("el 10/11/2026", RELOJ).fecha());
        assertEquals(LocalDate.of(2026, 10, 26), ConsultaMomento.interpretar("pasado mañana", RELOJ).fecha());
    }

    @Test void preguntaAntesDeAsumirTresDeLaMananaOTarde() {
        var resultado = ConsultaMomento.interpretar("¿Qué actividades hay hoy a las 3?", RELOJ);
        assertTrue(resultado.aclaracion().contains("a. m."));
        assertNull(resultado.hora());
    }

    @Test void validaSolapamientoYCuposEnElMomentoExacto() {
        var actual = Map.<String, Object>of("id", "a1", "fecha", "2026-10-25", "horaInicio", "14:00", "horaFin", "16:00", "estado", "programada", "cuposDisponibles", 4);
        var agotado = Map.<String, Object>of("id", "a2", "fecha", "2026-10-25", "horaInicio", "14:00", "horaFin", "16:00", "estado", "programada", "cuposDisponibles", 0);
        var siguiente = Map.<String, Object>of("id", "a3", "fecha", "2026-10-25", "horaInicio", "16:00", "horaFin", "17:00", "estado", "programada", "cuposDisponibles", 8);
        var opciones = List.of(actual, agotado, siguiente);
        assertEquals(List.of(actual), BusquedaActividadesMomento.buscar(opciones, LocalDate.of(2026, 10, 25), LocalTime.of(15, 0), null));
        assertEquals(List.of(siguiente), BusquedaActividadesMomento.buscar(opciones, LocalDate.of(2026, 10, 25), LocalTime.of(16, 0), null));
    }

    @Test void verificaFranjaYDescartaHorariosInvalidos() {
        var taller = Map.<String, Object>of("fecha", "2026-10-25", "horaInicio", "10:00", "horaFin", "11:00");
        var invalido = Map.<String, Object>of("fecha", "2026-10-25", "horaInicio", "texto", "horaFin", "11:00");
        assertEquals(List.of(taller), BusquedaActividadesMomento.buscar(List.of(taller, invalido), LocalDate.of(2026, 10, 25), LocalTime.of(9, 0), LocalTime.NOON));
    }
}
