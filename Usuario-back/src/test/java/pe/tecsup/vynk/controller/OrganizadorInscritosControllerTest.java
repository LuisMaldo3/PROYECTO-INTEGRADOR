package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.AsistenciaRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

class OrganizadorInscritosControllerTest {
    @Test void buscaPorNombreSinTildesYConservaTotalGeneral() {
        var acceso = mock(OrganizadorAccessService.class);
        var inscripciones = mock(InscripcionRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var asistencia = mock(AsistenciaRepository.class);
        var jwt = mock(Jwt.class);
        var organizador = new Usuario(); organizador.setId("org1");
        when(acceso.exigirOrganizador(jwt)).thenReturn(organizador);
        when(acceso.exigirActividadPropia(organizador, "act1")).thenReturn(new Document("_id", "act1"));
        when(acceso.resumen(any(Document.class))).thenReturn(java.util.Map.of("id", "act1"));
        var primera = new Inscripcion(); primera.setId("i1"); primera.setActividadId("act1"); primera.setEstudianteId("e1"); primera.setEstado("inscrita");
        var segunda = new Inscripcion(); segunda.setId("i2"); segunda.setActividadId("act1"); segunda.setEstudianteId("e2"); segunda.setEstado("inscrita");
        when(inscripciones.findByActividadId("act1")).thenReturn(List.of(primera, segunda));
        when(asistencia.findByActividadId("act1")).thenReturn(List.of());
        var maria = new Usuario(); maria.setNombre("María García"); maria.setCorreo("maria@tecsup.edu.pe");
        var carlos = new Usuario(); carlos.setNombre("Carlos Gómez"); carlos.setCorreo("carlos@tecsup.edu.pe");
        when(usuarios.findById("e1")).thenReturn(Optional.of(maria));
        when(usuarios.findById("e2")).thenReturn(Optional.of(carlos));
        var controller = new OrganizadorInscritosController(acceso, inscripciones, usuarios, asistencia);
        var resultado = controller.listar(jwt, "act1", "garcia");
        assertEquals(1, resultado.get("total"));
        assertEquals(2, resultado.get("totalInscritos"));
        assertEquals("María García", ((List<?>) resultado.get("inscritos")).stream()
                .map(a -> ((java.util.Map<?, ?>) a).get("nombre")).findFirst().orElse(null));
        verify(acceso).exigirActividadPropia(organizador, "act1");
    }
}
