package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

class OrganizadorEquiposVerificacionServiceTest {
    private final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizadorEquiposVerificacionService servicio =
            new OrganizadorEquiposVerificacionService(inscripciones, usuarios);

    private static Inscripcion miembro(String id, String alumno, String equipo, String rol) {
        Inscripcion i = new Inscripcion();
        i.setId(id); i.setEstudianteId(alumno); i.setEquipoId(equipo);
        i.setRolEquipo(rol); i.setEstado("inscrita");
        return i;
    }

    @Test void equipoConInscripcionesIndividualesVigentesEstaCompleto() {
        Inscripcion capitan = miembro("equipo1", "u1", "equipo1", "capitan");
        capitan.setIntegrantes(List.of("u1", "u2")); capitan.setNombreEquipo("Equipo Digital");
        Inscripcion integrante = miembro("i2", "u2", "equipo1", "integrante");
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of(capitan, integrante));
        Map<String, Object> datos = servicio.consultar("a1", 4);
        assertEquals(1, datos.get("totalEquipos"));
        assertEquals(1L, datos.get("equiposCompletos"));
        Map<?, ?> equipo = (Map<?, ?>) ((List<?>) datos.get("equipos")).get(0);
        assertEquals(true, equipo.get("completo"));
        assertEquals(2, equipo.get("inscripcionesConfirmadas"));
    }

    @Test void integranteSinInscripcionPersonalQuedaPendiente() {
        Inscripcion capitan = miembro("equipo1", "u1", "equipo1", "capitan");
        capitan.setIntegrantes(List.of("u1", "u2"));
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of(capitan));
        Map<String, Object> datos = servicio.consultar("a1", 4);
        assertEquals(1L, datos.get("equiposPendientes"));
        Map<?, ?> equipo = (Map<?, ?>) ((List<?>) datos.get("equipos")).get(0);
        assertEquals(1, equipo.get("inscripcionesPendientes"));
        assertEquals(false, equipo.get("completo"));
    }

    @Test void noCuentaInscripcionesCanceladasNiEquiposDeUnSoloIntegrante() {
        Inscripcion capitan = miembro("equipo1", "u1", "equipo1", "capitan");
        capitan.setIntegrantes(List.of("u1", "u2"));
        Inscripcion integrante = miembro("i2", "u2", "equipo1", "integrante");
        integrante.setEstado("cancelada");
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of(capitan, integrante));
        Map<String, Object> datos = servicio.consultar("a1", 4);
        assertEquals(0L, datos.get("equiposCompletos"));
        capitan.setIntegrantes(List.of("u1"));
        assertEquals(1L, servicio.consultar("a1", 4).get("equiposPendientes"));
    }

    @Test void excesoDeIntegrantesEInconsistenciaDeEquipoSeMarcanPendientes() {
        Inscripcion capitan = miembro("equipo1", "u1", "equipo1", "capitan");
        capitan.setIntegrantes(List.of("u1", "u2", "u3"));
        Inscripcion otro = miembro("i2", "u2", "otroEquipo", "integrante");
        Inscripcion tercero = miembro("i3", "u3", "equipo1", "integrante");
        when(inscripciones.findByActividadId("a1")).thenReturn(List.of(capitan, otro, tercero));
        Map<String, Object> datos = servicio.consultar("a1", 2);
        Map<?, ?> equipo = (Map<?, ?>) ((List<?>) datos.get("equipos")).get(0);
        assertEquals(false, equipo.get("completo"));
        assertEquals(true, equipo.get("superaLimite"));
        assertEquals(1, equipo.get("inscripcionesPendientes"));
    }
}
