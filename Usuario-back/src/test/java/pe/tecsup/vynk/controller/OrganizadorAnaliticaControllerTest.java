package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.ContextoSugerenciasActividadService;
import pe.tecsup.vynk.service.GeminiResumenInscripcionesService;
import pe.tecsup.vynk.service.GeminiSugerenciasActividadService;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorParticipacionService;

class OrganizadorAnaliticaControllerTest {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final OrganizadorParticipacionService participacion = mock(OrganizadorParticipacionService.class);
    private final GeminiResumenInscripcionesService gemini = mock(GeminiResumenInscripcionesService.class);
    private final ContextoSugerenciasActividadService contextoSugerencias = mock(ContextoSugerenciasActividadService.class);
    private final GeminiSugerenciasActividadService geminiSugerencias = mock(GeminiSugerenciasActividadService.class);
    private final OrganizadorAnaliticaController controller =
            new OrganizadorAnaliticaController(acceso, participacion, gemini, contextoSugerencias, geminiSugerencias);
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

    // --- Tareas 1 y 2: sugerencias para futuras actividades ---------------------------------------

    @Test void unCoordinadorDeAreaTambienPuedeVerLosDatosReunidos() {
        var coordinador = new Usuario(); coordinador.setId("org2");
        var actividad = new Document("_id", "a1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(coordinador);
        when(acceso.exigirActividadAsignada(coordinador, "a1")).thenReturn(actividad);
        Map<String, Object> datos = Map.of("nombreActividad", "Taller");
        when(contextoSugerencias.reunir(actividad)).thenReturn(datos);

        assertEquals(datos, controller.obtenerDatosSugerencias(jwt, "a1"));
        verifyNoInteractions(geminiSugerencias);
    }

    @Test void generarSugerenciasJuntaLosDatosYLosMandaAGemini() {
        var titular = new Usuario(); titular.setId("org1");
        var actividad = new Document("_id", "a1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(titular);
        when(acceso.exigirActividadAsignada(titular, "a1")).thenReturn(actividad);
        Map<String, Object> datos = Map.of("nombreActividad", "Taller", "totalComentarios", 5);
        when(contextoSugerencias.reunir(actividad)).thenReturn(datos);
        var ia = new GeminiSugerenciasActividadService.Respuesta(
                List.of(Map.of("titulo", "Ampliar horario", "detalle", "Faltó tiempo")), "Resumen breve", "gemini", "Revísalas");
        when(geminiSugerencias.generar(datos)).thenReturn(ia);

        var respuesta = controller.generarSugerenciasFuturas(jwt, "a1");

        assertEquals(datos, respuesta.get("datos"));
        assertEquals(ia.sugerencias(), respuesta.get("sugerencias"));
        assertEquals("Resumen breve", respuesta.get("resumen"));
        assertEquals("gemini", respuesta.get("origen"));
        assertEquals("Revísalas", respuesta.get("aviso"));
    }

    @Test void unOrganizadorSinAsignacionNoVeDatosNiPideSugerencias() {
        var ajeno = new Usuario(); ajeno.setId("org3");
        when(acceso.exigirOrganizador(jwt)).thenReturn(ajeno);
        when(acceso.exigirActividadAsignada(ajeno, "x")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> controller.obtenerDatosSugerencias(jwt, "x"));
        assertThrows(ResponseStatusException.class, () -> controller.generarSugerenciasFuturas(jwt, "x"));
        verifyNoInteractions(contextoSugerencias, geminiSugerencias);
    }
}
