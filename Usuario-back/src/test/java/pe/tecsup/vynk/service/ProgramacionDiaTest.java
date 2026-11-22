package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class ProgramacionDiaTest {
    static final LocalDate HOY = LocalDate.parse("2026-11-22");

    @Test void abreHoySiHayActividades() {
        assertEquals("2026-11-22", ProgramacionDiaService.diaInicial(Set.of("2026-11-20", "2026-11-22", "2026-11-25"), HOY));
    }

    @Test void siHoyNoHayAbreElProximoDiaConActividades() {
        assertEquals("2026-11-25", ProgramacionDiaService.diaInicial(Set.of("2026-11-20", "2026-11-27", "2026-11-25"), HOY));
    }

    @Test void siYaPasaronTodosAbreElUltimoYSinDiasAbreHoy() {
        assertEquals("2026-11-20", ProgramacionDiaService.diaInicial(Set.of("2026-11-18", "2026-11-20"), HOY));
        assertEquals("2026-11-22", ProgramacionDiaService.diaInicial(Set.of(), HOY));
    }

    static Map<String, Object> actividad(String estado, String inicio, String fin) {
        Map<String, Object> a = new HashMap<>();
        a.put("estado", estado); a.put("fecha", "2026-11-22"); a.put("horaInicio", inicio); a.put("horaFin", fin);
        return a;
    }

    @Test void calculaSiEstaPorEmpezarEnCursoOFinalizadaEnHoraDePeru() {
        Instant ahora = Instant.parse("2026-11-22T15:30:00Z"); // 10:30 en Lima
        assertEquals("proxima", ProgramacionDiaService.estadoDia(actividad("programada", "11:00", "12:00"), ahora));
        assertEquals("en_curso", ProgramacionDiaService.estadoDia(actividad("programada", "10:00", "11:00"), ahora));
        assertEquals("finalizada", ProgramacionDiaService.estadoDia(actividad("programada", "09:00", "10:30"), ahora));
        assertEquals("en_curso", ProgramacionDiaService.estadoDia(actividad("programada", "10:00", ""), ahora), "sin hora de fin sigue en curso");
    }

    @Test void elEstadoGuardadoMandaSobreLaHora() {
        Instant ahora = Instant.parse("2026-11-22T15:30:00Z");
        assertEquals("cancelada", ProgramacionDiaService.estadoDia(actividad("cancelada", "11:00", "12:00"), ahora));
        assertEquals("finalizada", ProgramacionDiaService.estadoDia(actividad("finalizada", "11:00", "12:00"), ahora));
        assertEquals("proxima", ProgramacionDiaService.estadoDia(actividad("programada", "", ""), ahora), "sin hora no se inventa");
    }

    // ---------- Ficha de ubicación (US-71) ----------

    @Test void laFichaIncluyeLugarYReferencia() {
        var ficha = UbicacionesActividadesService.ficha(new Document("_id", "auditorio").append("nombre", "Auditorio")
                .append("pabellon", "Pabellón A").append("referencia", "Frente a la biblioteca, 2.º piso"));
        assertEquals("Auditorio", ficha.get("lugar"));
        assertEquals("Pabellón A", ficha.get("pabellon"));
        assertEquals("Frente a la biblioteca, 2.º piso", ficha.get("referencia"));
        assertEquals(true, ficha.get("confirmada"));
        assertFalse(ficha.containsKey("latitud"), "sin coordenadas cargadas no se inventan");
    }

    @Test void soloIncluyeCoordenadasValidas() {
        var conCoordenadas = UbicacionesActividadesService.ficha(new Document("nombre", "Patio central")
                .append("latitud", -12.0445).append("longitud", "-76.9529"));
        assertEquals(-12.0445, conCoordenadas.get("latitud"));
        assertEquals(-76.9529, conCoordenadas.get("longitud"));
        var malas = UbicacionesActividadesService.ficha(new Document("nombre", "Patio").append("latitud", 200).append("longitud", "x"));
        assertFalse(malas.containsKey("latitud"));
        var media = UbicacionesActividadesService.ficha(new Document("nombre", "Patio").append("latitud", -12.0));
        assertFalse(media.containsKey("latitud"), "una sola coordenada no sirve");
    }

    @Test void sinUbicacionQuedaPorConfirmar() {
        var ficha = UbicacionesActividadesService.ficha(null);
        assertEquals("Ubicación por confirmar", ficha.get("lugar"));
        assertEquals(false, ficha.get("confirmada"));
        assertEquals("", ficha.get("referencia"));
    }
}
