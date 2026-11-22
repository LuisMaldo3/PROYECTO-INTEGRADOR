package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

class MiProximaYSugerenciasTest {
    static Inscripcion i(String actividad, String fecha, String inicio, String fin) {
        Inscripcion x = new Inscripcion();
        x.setActividadId(actividad); x.setFecha(fecha); x.setHoraInicio(inicio); x.setHoraFin(fin); x.setEstado("inscrita");
        return x;
    }

    // ---------- US-74 ----------

    @Test void laEnCursoVaPrimeroYLuegoLaQueEmpiezaAntes() {
        Instant ahora = Instant.parse("2026-11-23T15:30:00Z"); // 10:30 en Lima
        var orden = MiProximaActividadService.ordenar(List.of(
                i("tarde", "2026-11-23", "16:00", "17:00"),
                i("enCurso", "2026-11-23", "10:00", "11:00"),
                i("manana", "2026-11-24", "09:00", "10:00")), ahora);
        assertEquals(List.of("enCurso", "tarde", "manana"), orden.stream().map(t -> t.inscripcion().getActividadId()).toList());
    }

    @Test void descartaLasTerminadasYLasSinHora() {
        Instant ahora = Instant.parse("2026-11-23T15:30:00Z");
        var orden = MiProximaActividadService.ordenar(List.of(
                i("terminada", "2026-11-23", "08:00", "09:00"),
                i("sinHora", "2026-11-23", "", ""),
                i("sinFecha", "", "10:00", "")), ahora);
        assertTrue(orden.isEmpty());
    }

    @Test void sinHoraDeFinSeConsideraEnCursoUnaHora() {
        var dentro = MiProximaActividadService.ordenar(List.of(i("a", "2026-11-23", "10:00", "")), Instant.parse("2026-11-23T15:50:00Z"));
        assertEquals(1, dentro.size());
        var fuera = MiProximaActividadService.ordenar(List.of(i("a", "2026-11-23", "10:00", "")), Instant.parse("2026-11-23T16:00:00Z"));
        assertTrue(fuera.isEmpty());
    }

    // ---------- US-75 T01 ----------

    @Test void personalizaConMiProximaOtraActividadYUnInteres() {
        var s = SugerenciasAsistenteService.armar("Charla IA", "Torneo de fútbol", List.of("deportes"));
        assertEquals(List.of("¿Dónde se realiza Charla IA?", "¿Qué requisitos tiene Torneo de fútbol?",
                "¿Qué actividades de deportes hay mañana?", "¿Qué actividades hay hoy?", "¿Qué actividades hay mañana?"), s);
    }

    @Test void sinDatosDelEstudianteUsaPreguntasGenerales() {
        var s = SugerenciasAsistenteService.armar(null, null, List.of());
        assertEquals(SugerenciasAsistenteService.GENERALES, s);
    }

    @Test void nuncaRepiteNiPasaDeCinco() {
        var s = SugerenciasAsistenteService.armar("Charla IA", null, List.of("ia", "musica"));
        assertEquals(5, s.size());
        assertEquals(s.size(), s.stream().distinct().count());
        assertTrue(s.contains("¿Qué requisitos tiene Charla IA?"));
        assertTrue(s.contains("¿Qué actividades de inteligencia artificial hay mañana?"));
    }

    @Test void limpiaNombresYTemas() {
        assertEquals("Concurso de baile", SugerenciasAsistenteService.nombreLimpio("  «Concurso   de baile»? "));
        assertNull(SugerenciasAsistenteService.nombreLimpio("null"));
        assertEquals(60, SugerenciasAsistenteService.nombreLimpio("x".repeat(90)).length());
        assertEquals("tecnología", SugerenciasAsistenteService.temaDeInteres("tecnologia"));
        assertEquals("música", SugerenciasAsistenteService.temaDeInteres("musica"));
        assertNull(SugerenciasAsistenteService.temaDeInteres("no-existe"));
    }
}
