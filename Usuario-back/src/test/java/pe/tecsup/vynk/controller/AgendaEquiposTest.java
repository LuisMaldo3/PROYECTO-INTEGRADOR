package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;

/** US-77-T06: la agenda individual muestra una sola vez cada competencia de equipo. */
class AgendaEquiposTest {
    static Inscripcion insc(String id, String estudiante, String actividad, String rol) {
        Inscripcion i = new Inscripcion();
        i.setId(id); i.setEstudianteId(estudiante); i.setActividadId(actividad); i.setEstado("inscrita"); i.setRolEquipo(rol);
        return i;
    }

    @Test void elIntegranteVeSuPropiaInscripcionYNoLaDelCapitanRepetida() {
        Inscripcion propia = insc("m1", "ana", "hack", "integrante");
        Inscripcion delCapitan = insc("c1", "jordi", "hack", "capitan");
        delCapitan.setIntegrantes(new ArrayList<>(List.of("jordi", "ana")));
        Inscripcion individual = insc("i1", "ana", "charla", null);
        var r = AgendaController.unir(List.of(individual, propia), List.of(delCapitan), List.of());
        assertEquals(List.of("charla", "hack"), new ArrayList<>(r.keySet()));
        assertSame(propia, r.get("hack"));
    }

    @Test void conSoloUnaListaAntiguaSeMuestraLaDelEquipoYNoSiYaSalio() {
        Inscripcion delCapitan = insc("c1", "jordi", "hack", "capitan");
        assertSame(delCapitan, AgendaController.unir(List.of(), List.of(delCapitan), List.of()).get("hack"));
        assertTrue(AgendaController.unir(List.of(), List.of(delCapitan), List.of("hack")).isEmpty());
        delCapitan.setEstado("cancelada");
        assertTrue(AgendaController.unir(List.of(), List.of(delCapitan), List.of()).isEmpty());
    }
}
