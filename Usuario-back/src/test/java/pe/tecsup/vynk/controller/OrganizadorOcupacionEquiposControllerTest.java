package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorCuposService;
import pe.tecsup.vynk.service.OrganizadorEquiposVerificacionService;

class OrganizadorOcupacionEquiposControllerTest {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final OrganizadorCuposService cupos = mock(OrganizadorCuposService.class);
    private final OrganizadorEquiposVerificacionService equipos = mock(OrganizadorEquiposVerificacionService.class);
    private final OrganizadorOcupacionEquiposController controlador =
            new OrganizadorOcupacionEquiposController(acceso, cupos, equipos);
    private final Jwt jwt = mock(Jwt.class);

    private void preparar(Document actividad) {
        Usuario organizador = new Usuario(); organizador.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(organizador);
        when(acceso.exigirActividadPropia(organizador, "a1")).thenReturn(actividad);
    }

    @Test void calculaPorcentajeYSinModificarCapacidad() {
        preparar(new Document("_id", "a1"));
        Map<String, Object> resumen = Map.of("id", "a1", "maximoParticipantes", 8);
        when(acceso.resumen(any(Document.class))).thenReturn(resumen);
        when(cupos.enriquecer(resumen)).thenReturn(Map.of("maximoParticipantes", 8,
                "cuposOcupados", 3L, "cuposDisponibles", 5L, "unidadCupo", "estudiantes"));
        Map<String, Object> respuesta = controlador.ocupacion(jwt, "a1");
        assertEquals(38L, respuesta.get("porcentajeOcupacion"));
        assertEquals(false, respuesta.get("excedeLimite"));
        verify(acceso).exigirActividadPropia(any(Usuario.class), eq("a1"));
        verifyNoInteractions(equipos);
    }

    @Test void porcentajePuedeSuperarCienYNoOcultaExcesoDeCupos() {
        preparar(new Document("_id", "a1"));
        Map<String, Object> resumen = Map.of("id", "a1", "maximoParticipantes", 2);
        when(acceso.resumen(any(Document.class))).thenReturn(resumen);
        when(cupos.enriquecer(resumen)).thenReturn(Map.of("maximoParticipantes", 2,
                "cuposOcupados", 3L, "cuposDisponibles", 0L, "unidadCupo", "equipos"));
        var datos = controlador.ocupacion(jwt, "a1");
        assertEquals(150L, datos.get("porcentajeOcupacion"));
        assertEquals(true, datos.get("excedeLimite"));
    }

    @Test void rechazaVerificacionSiNoEsCompetenciaPorEquipos() {
        preparar(new Document("_id", "a1").append("tipoActividad", "taller")
                .append("modalidad", "individual"));
        assertThrows(ResponseStatusException.class, () -> controlador.verificarEquipos(jwt, "a1"));
        verifyNoInteractions(equipos);
    }

    @Test void consultaSoloEquiposDeActividadPropia() {
        preparar(new Document("_id", "a1").append("tipoActividad", "competencia")
                .append("modalidad", "equipos").append("maxIntegrantesEquipo", 5));
        when(equipos.consultar("a1", 5)).thenReturn(Map.of("totalEquipos", 0,
                "equiposCompletos", 0L, "equiposPendientes", 0L, "equipos", java.util.List.of()));
        assertEquals("a1", controlador.verificarEquipos(jwt, "a1").get("actividadId"));
        verify(equipos).consultar("a1", 5);
    }
}
