package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.ComentarioActividad;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.ComentarioActividadRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

class ComentariosActividadServiceTest {
    final ComentarioActividadRepository comentarios = mock(ComentarioActividadRepository.class);
    final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final ComentariosActividadService servicio = new ComentariosActividadService(comentarios, inscripciones, usuarios);
    final Jwt jwt = mock(Jwt.class);

    private Usuario participante() {
        when(jwt.getSubject()).thenReturn("alumno1");
        Usuario u = new Usuario(); u.setId("alumno1"); u.setRol("usuario"); u.setActivo(true);
        when(usuarios.findById("alumno1")).thenReturn(Optional.of(u));
        return u;
    }
    private void inscribir() {
        Inscripcion i = new Inscripcion(); i.setEstado("inscrita");
        when(inscripciones.findByEstudianteIdAndActividadId("alumno1", "actividad1")).thenReturn(Optional.of(i));
    }

    @Test void guardarComentarioExigeInscripcionPersonalActiva() {
        participante();
        var ex = assertThrows(ResponseStatusException.class, () ->
                servicio.guardar(jwt, "actividad1", "Buena organización"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(comentarios);
    }

    @Test void rechazaComentarioFueraDeLimites() {
        participante(); inscribir();
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> servicio.guardar(jwt, "actividad1", "corto")).getStatusCode());
        verify(comentarios, never()).save(any());
    }

    @Test void creaComentarioSinDuplicarInscripcion() {
        participante(); inscribir();
        when(comentarios.findByActividadIdAndEstudianteId("actividad1", "alumno1"))
                .thenReturn(Optional.empty());
        var d = servicio.guardar(jwt, "actividad1", "La explicación estuvo clara.");
        assertEquals(true, d.get("guardado"));
        verify(comentarios).save(argThat(c -> "actividad1".equals(c.getActividadId()) &&
                "alumno1".equals(c.getEstudianteId()) && c.getActualizadoEn() != null));
    }

    @Test void permiteEditarComentarioExistente() {
        participante(); inscribir();
        ComentarioActividad anterior = new ComentarioActividad(); anterior.setId("comentario1");
        when(comentarios.findByActividadIdAndEstudianteId("actividad1", "alumno1"))
                .thenReturn(Optional.of(anterior));
        servicio.guardar(jwt, "actividad1", "Mejoró la atención a participantes.");
        verify(comentarios).save(same(anterior));
        assertEquals("comentario1", anterior.getId());
    }

    @Test void noExponeComentarioSiLaInscripcionFueCancelada() {
        participante();
        Inscripcion cancelada = new Inscripcion(); cancelada.setEstado("cancelada");
        when(inscripciones.findByEstudianteIdAndActividadId("alumno1", "actividad1"))
                .thenReturn(Optional.of(cancelada));
        var estado = servicio.miComentario(jwt, "actividad1");
        assertEquals(false, estado.get("puedeComentar"));
        verifyNoInteractions(comentarios);
    }
}
