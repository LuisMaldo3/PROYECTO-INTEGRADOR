package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.GeminiResumenInscripcionesService;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorParticipacionService;

class OrganizadorAnaliticaControllerTest {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final OrganizadorParticipacionService participacion = mock(OrganizadorParticipacionService.class);
    private final GeminiResumenInscripcionesService gemini = mock(GeminiResumenInscripcionesService.class);
    private final OrganizadorAnaliticaController controller =
            new OrganizadorAnaliticaController(acceso, participacion, gemini);
    private final Jwt jwt = mock(Jwt.class);

    @Test void consultaIndicadoresSoloDeActividadPropia() {
        var titular = new Usuario(); titular.setId("org1");
        var actividad = new Document("_id", "a1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(titular);
        when(acceso.exigirActividadPropia(titular, "a1")).thenReturn(actividad);
        when(participacion.calcular(actividad)).thenReturn(Map.of("cuposOcupados", 3));
        assertEquals(3, controller.obtenerParticipacion(jwt, "a1").get("cuposOcupados"));
        verifyNoInteractions(gemini);
    }

    @Test void generarIaDevuelveCifrasInclusoSiLaClaveNoEstaConfigurada() {
        var titular = new Usuario(); titular.setId("org1");
        var actividad = new Document("_id", "a1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(titular);
        when(acceso.exigirActividadPropia(titular, "a1")).thenReturn(actividad);
        Map<String, Object> datos = Map.of("estudiantesInscritos", 8);
        when(participacion.calcular(actividad)).thenReturn(datos);
        when(gemini.resumir(datos)).thenReturn(new GeminiResumenInscripcionesService.Respuesta(
                "", "no_configurada", "Configura la API"));
        var respuesta = controller.generarResumen(jwt, "a1");
        assertEquals("no_configurada", respuesta.get("origen"));
        assertEquals(datos, respuesta.get("datos"));
        assertEquals("", respuesta.get("resumen"));
    }

    @Test void impideLeerActividadAjenaAntesDeLlamarGemini() {
        var titular = new Usuario(); titular.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(titular);
        when(acceso.exigirActividadPropia(titular, "ajena")).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> controller.generarResumen(jwt, "ajena"));
        verifyNoInteractions(participacion, gemini);
    }
}
