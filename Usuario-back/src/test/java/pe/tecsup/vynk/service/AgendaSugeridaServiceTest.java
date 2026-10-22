package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

class AgendaSugeridaServiceTest {
    static Map<String, Object> actividad(String id, String fecha, String inicio, String fin) {
        return Map.of("id", id, "nombre", "Actividad " + id, "fecha", fecha, "horaInicio", inicio, "horaFin", fin);
    }
    static Inscripcion inscripcion(String fecha, String inicio, String fin) {
        Inscripcion i = new Inscripcion(); i.setFecha(fecha); i.setHoraInicio(inicio); i.setHoraFin(fin);
        i.setEstado("inscrita"); return i;
    }
    @Test void noProponeCrucesConInscripcionesNiEntreSugerencias() {
        var inscritas = List.of(inscripcion("2026-12-01", "10:00", "11:00"));
        var lista = List.of(actividad("conflicto", "2026-12-01", "10:30", "12:00"),
                actividad("a", "2026-12-01", "11:00", "12:00"),
                actividad("b", "2026-12-01", "11:30", "13:00"),
                actividad("c", "2026-12-02", "10:00", "11:00"));
        var salida = AgendaSugeridaService.sinCruces(inscritas, lista, Map.of(), 6);
        assertEquals(List.of("a", "c"), salida.stream().map(a -> a.get("id")).toList());
    }
    @Test void rechazaHorasInvalidasYDuplicados() {
        var lista = List.of(actividad("invalida", "2026-12-01", "12:00", "10:00"),
                actividad("ok", "2026-12-01", "09:00", "10:00"),
                actividad("ok", "2026-12-01", "09:00", "10:00"));
        assertEquals(1, AgendaSugeridaService.sinCruces(List.of(), lista, Map.of(), 6).size());
    }
    @Test void horariosContiguosNoSonCruces() {
        var a = AgendaSugeridaService.horario("2026-12-01", "09:00", "10:00");
        var b = AgendaSugeridaService.horario("2026-12-01", "10:00", "11:00");
        assertNotNull(a); assertNotNull(b); assertFalse(a.cruza(b));
    }
}
