package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.ComentariosActividadService;
import pe.tecsup.vynk.service.GeminiComentariosService;
import pe.tecsup.vynk.service.OrganizadorAccessService;

class OrganizadorComentariosControllerTest {
    final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    final ComentariosActividadService comentarios = mock(ComentariosActividadService.class);
    final GeminiComentariosService ia = mock(GeminiComentariosService.class);
    final OrganizadorComentariosController controller = new OrganizadorComentariosController(acceso, comentarios, ia);
    final Jwt jwt = mock(Jwt.class);

    @Test void soloResponsablePuedeGenerarAnalisis() {
        var organizador = new Usuario(); organizador.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(organizador);
        when(acceso.exigirActividadPropia(organizador, "ajena"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> controller.analizar(jwt, "ajena"));
        verifyNoInteractions(comentarios, ia);
    }

    @Test void entregaResumenTemasYContadorSinIdentidades() {
        var organizador = new Usuario(); organizador.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(organizador);
        when(acceso.exigirActividadPropia(organizador, "a1")).thenReturn(new Document("_id", "a1"));
        when(comentarios.listar("a1")).thenReturn(List.of());
        when(ia.analizar(List.of())).thenReturn(new GeminiComentariosService.Respuesta(
                "Buena experiencia", List.of(), "gemini", "", 0));
        var r = controller.analizar(jwt, "a1");
        assertEquals("Buena experiencia", r.get("resumen"));
        assertEquals(0, r.get("totalComentarios"));
        assertFalse(r.containsKey("estudianteId"));
    }
}
