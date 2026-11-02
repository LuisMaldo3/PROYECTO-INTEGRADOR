package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

class OrganizadorReemplazosControllerTest {
    private final OrganizadorAccessService acceso = mock(OrganizadorAccessService.class);
    private final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final Jwt jwt = mock(Jwt.class);
    private final OrganizadorReemplazosController controlador =
            new OrganizadorReemplazosController(acceso, inscripciones, usuarios, mongo);

    private void preparar() {
        Usuario organizador = new Usuario(); organizador.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(organizador);
        when(acceso.exigirActividadPropia(organizador, "act1"))
                .thenReturn(new Document("_id", "act1")
                        .append("estado", "programada")
                        .append("modalidad", "individual")
                        .append("maximoParticipantes", 3)
                        .append("participantesInscritos", 3));
        Inscripcion saliente = new Inscripcion();
        saliente.setId("insc1"); saliente.setActividadId("act1");
        saliente.setEstudianteId("u1"); saliente.setEstado("inscrita");
        when(inscripciones.findById("insc1")).thenReturn(Optional.of(saliente));
        Usuario nuevo = new Usuario(); nuevo.setId("u2"); nuevo.setNombre("Nuevo estudiante");
        nuevo.setCorreo("nuevo@tecsup.edu.pe");
        when(usuarios.findByCorreo("nuevo@tecsup.edu.pe")).thenReturn(Optional.of(nuevo));
        when(inscripciones.findByEstudianteIdAndActividadId("u2", "act1"))
                .thenReturn(Optional.empty());
        Inscripcion resultante = new Inscripcion(); resultante.setId("insc1");
        when(mongo.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Inscripcion.class))).thenReturn(resultante);
    }

    @Test void reemplazaSinCambiarLimiteNiContadorDeCupos() {
        preparar();
        var resultado = controlador.reemplazar(jwt, "act1",
                new OrganizadorReemplazosController.Solicitud("insc1", "nuevo@tecsup.edu.pe", "Cambio autorizado"));
        assertEquals(true, resultado.get("reemplazado"));
        assertEquals(false, resultado.get("cuposModificados"));
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq("actividades"));
        verify(mongo, never()).updateMulti(any(Query.class), any(Update.class), eq("actividades"));
    }

    @Test void noPermiteParticipanteDuplicado() {
        preparar();
        Inscripcion yaExistente = new Inscripcion(); yaExistente.setId("insc2");
        when(inscripciones.findByEstudianteIdAndActividadId("u2", "act1"))
                .thenReturn(Optional.of(yaExistente));
        assertThrows(ResponseStatusException.class, () -> controlador.reemplazar(jwt, "act1",
                new OrganizadorReemplazosController.Solicitud("insc1", "nuevo@tecsup.edu.pe", "Corrección solicitada")));
        verify(mongo, never()).findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Inscripcion.class));
    }

    @Test void noPermiteActividadAjena() {
        Usuario responsable = new Usuario(); responsable.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(responsable);
        when(acceso.exigirActividadPropia(responsable, "ajena"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertThrows(ResponseStatusException.class, () -> controlador.reemplazar(jwt, "ajena",
                new OrganizadorReemplazosController.Solicitud("insc1", "nuevo@tecsup.edu.pe", "Cambio autorizado")));
        verify(mongo, never()).findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(Inscripcion.class));
    }
}
