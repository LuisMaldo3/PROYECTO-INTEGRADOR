package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.model.Inscripcion;

class SugerenciasHuecosServiceTest {
    static Inscripcion inscrito(String dia, String inicio, String fin) {
        var i = new Inscripcion(); i.setFecha(dia); i.setHoraInicio(inicio); i.setHoraFin(fin); return i;
    }
    @Test void descuentaClasesInscripcionesYGeneraHuecos() {
        // 2026-11-02 es lunes. Dentro de 08:00-21:00 hay tres huecos válidos.
        var clases = List.of(new BloqueOcupado(1, "08:00", "10:00", "Laboratorio"));
        var lista = SugerenciasHuecosService.huecos(LocalDate.parse("2026-11-02"), clases,
            List.of(inscrito("2026-11-02", "13:00", "15:00")));
        assertEquals(2, lista.size());
        assertEquals("10:00", lista.get(0).inicio().toString());
        assertEquals("13:00", lista.get(0).fin().toString());
        assertEquals("15:00", lista.get(1).inicio().toString());
    }
    @Test void rechazaActividadQueNoCabeYPermiteLimitesExactos() {
        var libres = SugerenciasHuecosService.huecos(LocalDate.parse("2026-11-02"),
            List.of(new BloqueOcupado(1, "10:00", "12:00", "Clase")), List.of());
        assertTrue(SugerenciasHuecosService.cabe(Map.of("horaInicio", "12:00", "horaFin", "13:00"), libres));
        assertFalse(SugerenciasHuecosService.cabe(Map.of("horaInicio", "09:00", "horaFin", "11:00"), libres));
    }
    @Test void mergeDeBloquesSolapadosYLimites() {
        var libres = SugerenciasHuecosService.huecos(LocalDate.parse("2026-11-02"),
            List.of(new BloqueOcupado(1, "08:00", "11:00", "A"), new BloqueOcupado(1, "09:00", "12:00", "B")), List.of());
        assertEquals(1, libres.size());
        assertEquals("12:00", libres.get(0).inicio().toString());
    }
    @Test void verificaYOrdenaBloquesConfirmados() {
        var datos = DisponibilidadService.normalizar(List.of(
            new BloqueOcupado(3, "14:00", "16:00", "Taller"),
            new BloqueOcupado(1, "08:00", "10:00", "Clase")));
        assertEquals(1, datos.get(0).dia());
        assertThrows(IllegalArgumentException.class, () -> DisponibilidadService.normalizar(List.of(new BloqueOcupado(8, "10:00", "12:00", "Error"))));
    }
    @Test void filtraCrucesConClasesInscripcionesYEntreDias() {
        var clase = new BloqueOcupado(1, "09:00", "11:00", "Clase de software");
        var inscritas = List.of(inscrito("2026-11-02", "13:00", "14:00"));
        var candidatas = List.of(
            Map.<String, Object>of("id", "1", "fecha", "2026-11-02", "horaInicio", "10:00", "horaFin", "11:30"),
            Map.<String, Object>of("id", "2", "fecha", "2026-11-02", "horaInicio", "11:00", "horaFin", "12:00"),
            Map.<String, Object>of("id", "3", "fecha", "2026-11-02", "horaInicio", "13:30", "horaFin", "14:30"),
            Map.<String, Object>of("id", "4", "fecha", "2026-11-03", "horaInicio", "10:00", "horaFin", "11:00"),
            Map.<String, Object>of("id", "5", "fecha", "invalida", "horaInicio", "10:00", "horaFin", "11:00")
        );
        var salida = SugerenciasHuecosService.filtrarCompatibles(candidatas, List.of(clase), inscritas);
        assertEquals(List.of("2", "4"), salida.stream().map(a -> a.get("id")).toList());
    }
    @Test void mantieneBloquesContiguosYDescartaHorasFueraDeJornada() {
        var candidatas = List.of(
            Map.<String,Object>of("id", "a", "fecha", "2026-11-02", "horaInicio", "08:00", "horaFin", "09:00"),
            Map.<String,Object>of("id", "b", "fecha", "2026-11-02", "horaInicio", "20:30", "horaFin", "21:30"),
            Map.<String,Object>of("id", "c", "fecha", "2026-11-02", "horaInicio", "21:00", "horaFin", "22:00")
        );
        assertEquals(List.of("a"), SugerenciasHuecosService.filtrarCompatibles(candidatas,
            List.of(new BloqueOcupado(1, "09:00", "11:00", "Ocupado")), List.of())
            .stream().map(a -> a.get("id")).toList());
    }
}
