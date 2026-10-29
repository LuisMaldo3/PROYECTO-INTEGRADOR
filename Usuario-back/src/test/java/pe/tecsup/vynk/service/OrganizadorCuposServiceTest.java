package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;

class OrganizadorCuposServiceTest {
    @Test void cuentaInscritosActivosYNoCuposDeInscripcionesCanceladas() {
        InscripcionRepository repo = mock(InscripcionRepository.class);
        Inscripcion activa = new Inscripcion(); activa.setEstado("inscrita");
        Inscripcion cancelada = new Inscripcion(); cancelada.setEstado("cancelada");
        when(repo.findByActividadId("act1")).thenReturn(List.of(activa, cancelada));
        var resultado = new OrganizadorCuposService(repo).enriquecer(Map.of("id", "act1", "maximoParticipantes", 3));
        assertEquals(1L, resultado.get("cuposOcupados"));
        assertEquals(2L, resultado.get("cuposDisponibles"));
        assertEquals(1L, resultado.get("inscritosRegistrados"));
    }
    @Test void maximaCapacidadNoNegativaYCuposSinMaximoSonDesconocidos() {
        assertNull(OrganizadorCuposService.capacidad(null));
        assertEquals(12, OrganizadorCuposService.capacidad("12"));
        assertNull(OrganizadorCuposService.capacidad("incorrecto"));
    }
}
