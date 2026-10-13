package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;
class CruceHorarioServiceTest {
    private Inscripcion actividad(String id, String inicio, String fin, String estado) {
        Inscripcion a = new Inscripcion(); a.setActividadId(id); a.setNombreActividad(id);
        a.setFecha("2026-11-03"); a.setHoraInicio(inicio); a.setHoraFin(fin); a.setEstado(estado); return a;
    }
    @Test void detectaSolapamientoReal() {
        var cruces = new CruceHorarioService().detectar(List.of(actividad("A","10:00","12:00","inscrita"), actividad("B","11:00","13:00","inscrita")));
        assertEquals(1, cruces.size()); assertEquals(60, cruces.get(0).minutos());
    }
    @Test void noDetectaActividadesContiguas() {
        assertTrue(new CruceHorarioService().detectar(List.of(actividad("A","10:00","11:00","inscrita"), actividad("B","11:00","12:00","inscrita"))).isEmpty());
    }
    @Test void excluyeCanceladas() {
        assertTrue(new CruceHorarioService().detectar(List.of(actividad("A","10:00","12:00","cancelada"), actividad("B","11:00","13:00","inscrita"))).isEmpty());
    }
}
