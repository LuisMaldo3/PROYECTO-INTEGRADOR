package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

class DispositivosServiceTest {
    static final Instant AHORA = Instant.parse("2026-11-22T13:00:00Z");
    static final String TOKEN = "fcm-token:APA91b-0123456789_abcdefghij";

    final DispositivoMovilRepository dispositivos = mock(DispositivoMovilRepository.class);
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final DispositivosService servicio = new DispositivosService(dispositivos, usuarios);

    Usuario usuario(String id, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id); u.setCorreo(id + "@tecsup.edu.pe"); u.setActivo(activo);
        when(usuarios.findByCorreo(id + "@tecsup.edu.pe")).thenReturn(Optional.of(u));
        return u;
    }

    @Test void registraElCelularDeQuienTieneLaSesion() {
        usuario("u1", true);
        when(dispositivos.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(dispositivos.findByUsuarioId("u1")).thenReturn(List.of());

        servicio.registrar("u1@tecsup.edu.pe", TOKEN, AHORA);

        var captura = ArgumentCaptor.forClass(DispositivoMovil.class);
        verify(dispositivos).save(captura.capture());
        assertEquals("u1", captura.getValue().getUsuarioId());
        assertEquals(TOKEN, captura.getValue().getToken());
        assertEquals(AHORA, captura.getValue().getCreadoEn());
        assertEquals(AHORA, captura.getValue().getActualizadoEn());
    }

    @Test void siOtraCuentaEntraEnElMismoCelularElTokenPasaALaNueva() {
        usuario("u2", true);
        DispositivoMovil existente = new DispositivoMovil();
        existente.setId("d1"); existente.setUsuarioId("u1"); existente.setToken(TOKEN);
        existente.setCreadoEn(AHORA.minusSeconds(3600));
        when(dispositivos.findByToken(TOKEN)).thenReturn(Optional.of(existente));
        when(dispositivos.findByUsuarioId("u2")).thenReturn(List.of(existente));

        servicio.registrar("u2@tecsup.edu.pe", TOKEN, AHORA);

        verify(dispositivos).save(existente);
        assertEquals("u2", existente.getUsuarioId());
        assertEquals(AHORA.minusSeconds(3600), existente.getCreadoEn());
        assertEquals(AHORA, existente.getActualizadoEn());
    }

    @Test void guardaComoMaximoCincoCelularesPorEstudianteOlvidandoLosMasViejos() {
        usuario("u1", true);
        when(dispositivos.findByToken(TOKEN)).thenReturn(Optional.empty());
        List<DispositivoMovil> todos = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            DispositivoMovil d = new DispositivoMovil();
            d.setId("d" + i); d.setUsuarioId("u1"); d.setActualizadoEn(AHORA.minusSeconds(1000 - i));
            todos.add(d);
        }
        when(dispositivos.findByUsuarioId("u1")).thenReturn(todos);

        servicio.registrar("u1@tecsup.edu.pe", TOKEN, AHORA);

        verify(dispositivos).deleteById("d0");
        verify(dispositivos, times(1)).deleteById(any());
    }

    @Test void rechazaUnTokenQueNoTieneFormaDeTokenDeFirebase() {
        usuario("u1", true);
        for (String malo : new String[]{null, "", "   ", "corto", "tiene espacios en medio 0123456789", "<script>alert(1)</script>"}) {
            var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar("u1@tecsup.edu.pe", malo, AHORA));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
        verify(dispositivos, never()).save(any());
    }

    @Test void unaCuentaInactivaONoRegistradaNoPuedeRegistrarCelulares() {
        usuario("u1", false);
        when(usuarios.findByCorreo("nadie@tecsup.edu.pe")).thenReturn(Optional.empty());
        when(usuarios.findById("nadie@tecsup.edu.pe")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class, () -> servicio.registrar("u1@tecsup.edu.pe", TOKEN, AHORA)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> servicio.registrar("nadie@tecsup.edu.pe", TOKEN, AHORA)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                assertThrows(ResponseStatusException.class, () -> servicio.registrar(null, TOKEN, AHORA)).getStatusCode());
        verify(dispositivos, never()).save(any());
    }

    @Test void alCerrarSesionOlvidaElCelularSoloSiEraDeEsaCuenta() {
        usuario("u1", true);
        DispositivoMovil propio = new DispositivoMovil();
        propio.setId("d1"); propio.setUsuarioId("u1"); propio.setToken(TOKEN);
        when(dispositivos.findByToken(TOKEN)).thenReturn(Optional.of(propio));

        servicio.quitar("u1@tecsup.edu.pe", TOKEN);
        verify(dispositivos).deleteById("d1");

        // Ese mismo token pasó a otra cuenta: cerrar la sesión de u1 no debe quitarlo.
        clearInvocations(dispositivos);
        propio.setUsuarioId("u2");
        when(dispositivos.findByToken(TOKEN)).thenReturn(Optional.of(propio));
        servicio.quitar("u1@tecsup.edu.pe", TOKEN);
        verify(dispositivos, never()).deleteById(any());
    }
}
