package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

class BuscadorAlternativasCruceTest {
    private static final String FECHA = LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(8).toString();
    private static Inscripcion inscrita(String id, String inicio, String fin) {
        Inscripcion i = new Inscripcion();
        i.setActividadId(id); i.setNombreActividad(id); i.setFecha(FECHA);
        i.setHoraInicio(inicio); i.setHoraFin(fin); i.setCategoria("arte");
        return i;
    }
    private static Map<String,Object> candidata(String id, String fecha, String inicio, String fin, int cupos) {
        return Map.of("id", id, "nombre", "Arte " + id, "estado", "programada", "fecha", fecha,
                "horaInicio", inicio, "horaFin", fin, "categoriaId", "arte", "cuposDisponibles", cupos);
    }
    private final CruceHorarioService detector = new CruceHorarioService();
    private final BuscadorAlternativasCruce buscador = new BuscadorAlternativasCruce();

    @Test void excluyeCrucesInscritasYCuposAgotados() {
        var agenda = List.of(inscrita("A", "10:00", "12:00"), inscrita("B", "11:00", "13:00"), inscrita("C", "14:00", "15:00"));
        var cruce = detector.detectar(agenda).get(0);
        var opciones = List.of(candidata("X", FECHA, "09:00", "10:00", 2),
                candidata("Y", FECHA, "12:00", "14:30", 2), candidata("Z", FECHA, "15:00", "16:00", 0),
                candidata("C", FECHA, "16:00", "17:00", 3));
        var resultado = buscador.buscar(cruce, agenda, opciones);
        assertEquals(1, resultado.size());
        assertEquals("X", resultado.get(0).get("id"));
    }

    @Test void admiteBloquesContiguosYExcluyeFechasPasadas() {
        var agenda = List.of(inscrita("A", "10:00", "12:00"), inscrita("B", "11:00", "13:00"));
        var cruce = detector.detectar(agenda).get(0);
        var resultado = buscador.buscar(cruce, agenda, List.of(
                candidata("antes", FECHA, "09:00", "10:00", 2),
                candidata("despues", FECHA, "13:00", "14:00", 2),
                candidata("pasado", "2020-01-01", "13:00", "14:00", 2)));
        assertEquals(2, resultado.size());
    }

    @Test void noDevuelveOpcionesSiNoExisteElCruce() {
        assertTrue(buscador.buscar(null, List.of(), List.of()).isEmpty());
    }
}
