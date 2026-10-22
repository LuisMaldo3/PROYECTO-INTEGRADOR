package pe.tecsup.vynk.service;

import org.junit.jupiter.api.Test;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InteresRepository;
import pe.tecsup.vynk.repository.InteresesDeUsuarioRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Casos del primer ingreso y la elección posterior, sin requerir MongoDB real. */
class InteresesOnboardingTest {

    @Test
    void distinguePendienteOmitidoYConfigurado() {
        assertFalse(InteresesService.tieneInteresesElegidos(null));
        assertFalse(InteresesService.tieneInteresesElegidos(new InteresesDeUsuario("1", List.of(), null)));
        assertFalse(InteresesService.tieneInteresesElegidos(new InteresesDeUsuario("1", List.of(), Instant.now())));
        assertTrue(InteresesService.tieneInteresesElegidos(new InteresesDeUsuario("1", List.of("teatro"), Instant.now())));
    }

    @Test
    void omitirSinSeleccionGuardaEstadoSinCrearIntereses() {
        InteresRepository catalogo = mock(InteresRepository.class);
        InteresesDeUsuarioRepository elegidos = mock(InteresesDeUsuarioRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        when(usuarios.findById("u1")).thenReturn(Optional.of(usuario));
        when(elegidos.findById("u1")).thenReturn(Optional.empty());
        when(elegidos.save(any(InteresesDeUsuario.class))).thenAnswer(i -> i.getArgument(0));

        InteresesDeUsuario estado = new InteresesService(catalogo, elegidos, usuarios).omitir("u1");

        assertTrue(estado.isOmitido());
        assertEquals(List.of(), estado.getIntereses());
        assertFalse(InteresesService.tieneInteresesElegidos(estado));
        verify(elegidos).save(estado);
    }

    @Test
    void omitirConSeleccionPreviaNoLaBorra() {
        InteresesDeUsuario anterior = new InteresesDeUsuario("u1", List.of("musica"), Instant.now());
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setId("u1");
        when(usuarios.findById("u1")).thenReturn(Optional.of(usuario));
        InteresesDeUsuarioRepository elegidos = mock(InteresesDeUsuarioRepository.class);
        when(elegidos.findById("u1")).thenReturn(Optional.of(anterior));
        InteresesService servicio = new InteresesService(mock(InteresRepository.class), elegidos, usuarios);

        assertSame(anterior, servicio.omitir("u1"));
        assertEquals(List.of("musica"), anterior.getIntereses());
        verify(elegidos, never()).save(any());
    }

    @Test
    void cuentaDesactivadaNoPuedeOmitir() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setActivo(false);
        when(usuarios.findById("u1")).thenReturn(Optional.of(usuario));
        InteresesDeUsuarioRepository elegidos = mock(InteresesDeUsuarioRepository.class);
        InteresesService servicio = new InteresesService(mock(InteresRepository.class), elegidos, usuarios);
        assertThrows(CredencialInvalidaException.class, () -> servicio.omitir("u1"));
        verifyNoInteractions(elegidos);
    }
}
