package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorDetalleService;

class OrganizadorEdicionControllerTest {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final OrganizadorDetalleService detalles = mock(OrganizadorDetalleService.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final OrganizadorEdicionController controlador = new OrganizadorEdicionController(acceso, detalles, mongo);
    private final Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
            Map.of("alg", "HS256"), Map.of("sub", "organizador1"));

    private void autorizado(Document actividad) {
        Usuario usuario = new Usuario(); usuario.setId("organizador1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(usuario);
        when(acceso.exigirActividadPropia(usuario, "a1")).thenReturn(actividad);
    }

    @Test void niegaCambiosDeCuposFechasYPropietario() {
        autorizado(new Document("_id", "a1").append("tipoActividad", "competencia"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controlador.guardar(jwt, "a1", Map.of("maximoParticipantes", 99)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(mongo);
    }

    @Test void soloActualizaCamposPermitidos() {
        autorizado(new Document("_id", "a1").append("estado", "programada")
                .append("tipoActividad", "competencia"));
        when(mongo.findAndModify(any(Query.class), any(Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class),
                eq(Document.class), eq("actividades"))).thenReturn(new Document("_id", "a1"));
        when(detalles.detalle(jwt, "a1")).thenReturn(Map.of("id", "a1"));
        assertEquals("a1", controlador.guardar(jwt, "a1",
                Map.of("descripcion", "Información de prueba para actividad", "reglasCompetencia", "Reglas vigentes")).get("id"));
        verify(mongo).findAndModify(any(Query.class), any(Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class),
                eq(Document.class), eq("actividades"));
    }

    @Test void noEditaActividadesCanceladasNiFinalizadas() {
        autorizado(new Document("_id", "a1").append("estado", "finalizada"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controlador.guardar(jwt, "a1", Map.of("descripcion", "Descripción suficientemente larga")));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(mongo);
    }
}
