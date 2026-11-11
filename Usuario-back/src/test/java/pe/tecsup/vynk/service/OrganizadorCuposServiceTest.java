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
        assertEquals(1L, resultado.get("participantesTotales"));
    }
    @Test void maximaCapacidadNoNegativaYCuposSinMaximoSonDesconocidos() {
        assertNull(OrganizadorCuposService.capacidad(null));
        assertEquals(12, OrganizadorCuposService.capacidad("12"));
        assertNull(OrganizadorCuposService.capacidad("incorrecto"));
    }

    @Test void competenciasPorEquiposNoCobranUnCupoPorIntegrante() {
        InscripcionRepository repo = mock(InscripcionRepository.class);
        Inscripcion capitan = new Inscripcion();
        capitan.setId("equipo1"); capitan.setEstudianteId("u1"); capitan.setRolEquipo("capitan");
        capitan.setEstado("inscrita"); capitan.setNombreEquipo("Equipo Mecánica");
        capitan.setIntegrantes(List.of("u1", "u2")); capitan.setEquipoId("equipo1");
        Inscripcion integrante = new Inscripcion();
        integrante.setId("m2"); integrante.setEstudianteId("u2"); integrante.setRolEquipo("integrante");
        integrante.setEquipoId("equipo1"); integrante.setEstado("inscrita");
        when(repo.findByActividadId("futbol")).thenReturn(List.of(capitan, integrante));
        var datos = new OrganizadorCuposService(repo).enriquecer(Map.of("id", "futbol", "tipoActividad", "competencia", "modalidad", "equipos", "maximoParticipantes", 3));
        assertEquals(1L, datos.get("cuposOcupados"));
        assertEquals(2L, datos.get("cuposDisponibles"));
        assertEquals(2L, datos.get("estudiantesInscritos"));
        assertEquals(2L, datos.get("participantesTotales"));
        assertEquals(1L, datos.get("participantesCompetitivos"));
        assertEquals(1L, datos.get("equiposRegistrados"));
        assertEquals(1L, datos.get("equiposConfirmados"));
    }

    @Test void capitanSinOtroInscritoQuedaPendiente() {
        InscripcionRepository repo = mock(InscripcionRepository.class);
        Inscripcion capitan = new Inscripcion();
        capitan.setId("equipo1"); capitan.setEstudianteId("u1"); capitan.setRolEquipo("capitan");
        capitan.setEstado("inscrita"); capitan.setIntegrantes(List.of("u1")); capitan.setEquipoId("equipo1");
        when(repo.findByActividadId("futbol")).thenReturn(List.of(capitan));
        var datos = new OrganizadorCuposService(repo).enriquecer(Map.of("id", "futbol", "tipoActividad", "competencia", "modalidad", "equipos", "maximoParticipantes", 3));
        assertEquals(1L, datos.get("equiposPendientes"));
        assertEquals(0L, datos.get("equiposConfirmados"));
    }
}
