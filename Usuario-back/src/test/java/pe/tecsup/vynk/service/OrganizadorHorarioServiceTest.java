package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class OrganizadorHorarioServiceTest {
    @Test void admiteFechaHoraValidasYRechazaValoresMalFormados() {
        assertNotNull(OrganizadorHorarioService.instante(Map.of("fecha", "2026-10-30", "horaInicio", "10:30")));
        assertNull(OrganizadorHorarioService.instante(Map.of("fecha", "no fecha", "horaInicio", "10:30")));
        assertNull(OrganizadorHorarioService.instante(Map.of("fecha", "2026-10-30", "horaInicio", "-")));
    }

    @Test void resuelveUbicacionSinMostrarIdentificador() {
        UbicacionesActividadesService ubicaciones = mock(UbicacionesActividadesService.class);
        when(ubicaciones.resolver(anyList())).thenReturn(List.of(Map.of("confirmada", true, "lugar", "Auditorio", "sede", "Lima")));
        var servicio = new OrganizadorHorarioService(ubicaciones);
        var actividad = new LinkedHashMap<String, Object>();
        actividad.put("id", "abc");
        actividad.put("ubicacionId", "507f1f77bcf86cd799439011");
        var vista = servicio.enriquecer(actividad);
        assertEquals("Auditorio", vista.get("ubicacion"));
        assertEquals("507f1f77bcf86cd799439011", vista.get("ubicacionId"));
    }
}
