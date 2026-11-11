package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

class OrganizadorResultadosUs48Test {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final InscripcionRepository repo = mock(InscripcionRepository.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final OrganizadorResultadosController controller = new OrganizadorResultadosController(acceso, repo, mongo);
    private final Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
            Map.of("alg", "HS256"), Map.of("sub", "org1"));

    private Usuario autorizado() {
        Usuario responsable = new Usuario(); responsable.setId("org1"); responsable.setRol("organizador");
        when(acceso.exigirOrganizador(jwt)).thenReturn(responsable);
        when(acceso.exigirActividadPropia(responsable, "act1"))
                .thenReturn(new Document("tipoActividad", "competencia").append("estado", "programada"));
        return responsable;
    }

    private void inscrito() {
        Inscripcion i = new Inscripcion(); i.setId("in1"); i.setEstudianteId("al1");
        i.setEstado("inscrita"); i.setNombreEstudiante("Ana"); i.setAreaTecsup("Mecánica");
        i.setAreaVerificada(true);
        when(repo.findByActividadId("act1")).thenReturn(List.of(i));
    }

    @Test void noAdmitePuestosDuplicadosNiCompetidoresInexistentes() {
        autorizado(); inscrito();
        var repetido = List.of(Map.of("inscripcionId", "in1", "puesto", 1),
                Map.of("inscripcionId", "in1", "puesto", 2));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.enviar(jwt, "act1", Map.of("posiciones", repetido)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verifyNoInteractions(mongo);
    }

    @Test void soloAdmiteEvidenciaEnHttpsYNoPublicaDesdeElOrganizador() {
        autorizado(); inscrito();
        var puestos = List.of(Map.of("inscripcionId", "in1", "puesto", 1,
                "puntos", 10, "evidencia", "http://inseguro.com/acta"));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.enviar(jwt, "act1", Map.of("posiciones", puestos, "estado", "publicado")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verifyNoInteractions(mongo);
    }

    @Test void soloCoordinadoresDeAreaPuedenRevisar() {
        Usuario responsable = autorizado();
        when(acceso.exigirActividadAsignada(responsable, "act1"))
                .thenReturn(new Document("tipoActividad", "competencia"));
        when(acceso.areaCoordinador(any(Document.class), eq(responsable))).thenReturn(null);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.revisarArea(jwt, "act1", Map.of("estado", "conforme")));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verifyNoInteractions(mongo);
    }
    @Test void guardarPropuestaNuncaMarcaPublicadoAunqueClienteLoPida() {
        autorizado(); inscrito();
        var indexOps = mock(org.springframework.data.mongodb.core.index.IndexOperations.class);
        when(mongo.indexOps("resultados_competencias")).thenReturn(indexOps);
        when(mongo.findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class),
                eq(Document.class), eq("resultados_competencias")))
                .thenReturn(new Document("estado", "pendiente"));
        var puestos = List.of(Map.of("inscripcionId", "in1", "puesto", 1,
                "puntos", 10, "evidencia", "https://tecsup.edu.pe/evidencia"));
        var resultado = controller.enviar(jwt, "act1", Map.of("posiciones", puestos, "estado", "publicado"));
        assertEquals("pendiente", resultado.get("estado"));
        var cambios = org.mockito.ArgumentCaptor.forClass(org.springframework.data.mongodb.core.query.Update.class);
        verify(mongo).findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                cambios.capture(), any(org.springframework.data.mongodb.core.FindAndModifyOptions.class),
                eq(Document.class), eq("resultados_competencias"));
        var set = (Document) cambios.getValue().getUpdateObject().get("$set");
        assertEquals("pendiente", set.get("estado"));
    }

    @Test void rechazaSobrescrituraCuandoPropuestaFuePublicada() {
        autorizado(); inscrito();
        var indexOps = mock(org.springframework.data.mongodb.core.index.IndexOperations.class);
        when(mongo.indexOps("resultados_competencias")).thenReturn(indexOps);
        when(mongo.findAndModify(any(org.springframework.data.mongodb.core.query.Query.class),
                any(org.springframework.data.mongodb.core.query.Update.class),
                any(org.springframework.data.mongodb.core.FindAndModifyOptions.class),
                eq(Document.class), eq("resultados_competencias")))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("resultados publicados"));
        var puestos = List.of(Map.of("inscripcionId", "in1", "puesto", 1,
                "puntos", 10));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> controller.enviar(jwt, "act1", Map.of("posiciones", puestos)));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }
}
