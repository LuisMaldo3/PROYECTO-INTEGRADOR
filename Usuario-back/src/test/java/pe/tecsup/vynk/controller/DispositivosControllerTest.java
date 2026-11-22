package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.tecsup.vynk.service.DispositivosService;

class DispositivosControllerTest {
    static final String TOKEN = "fcm-token:APA91b-0123456789_abcdefghij";
    final DispositivosService servicio = mock(DispositivosService.class);
    final DispositivosController controller = new DispositivosController(servicio);
    final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    Jwt jwt(String sujeto) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(sujeto);
        return jwt;
    }

    @Test void registraElCelularDeLaSesionYResponde204() {
        var respuesta = controller.registrar(jwt("u1@tecsup.edu.pe"), new DispositivosController.Solicitud(TOKEN));

        assertEquals(204, respuesta.getStatusCode().value());
        verify(servicio).registrar(eq("u1@tecsup.edu.pe"), eq(TOKEN), any());
    }

    @Test void laBajaSoloUsaElUsuarioDelTokenDeSesion() {
        var respuesta = controller.baja(jwt("u1@tecsup.edu.pe"), new DispositivosController.Solicitud(TOKEN));

        assertEquals(204, respuesta.getStatusCode().value());
        verify(servicio).quitar("u1@tecsup.edu.pe", TOKEN);
    }

    @Test void sinSesionElServicioRecibeUnSujetoNuloYLoRechaza() {
        controller.registrar(null, new DispositivosController.Solicitud(TOKEN));
        verify(servicio).registrar(eq(null), eq(TOKEN), any());
    }

    @Test void laSolicitudAceptaTokensDeFirebaseYRechazaElResto() {
        assertTrue(validador.validate(new DispositivosController.Solicitud(TOKEN)).isEmpty());
        for (String malo : new String[]{null, "", "corto", "con espacios 0123456789abcdef", "x".repeat(5000), "<b>0123456789abcdefghij</b>"})
            assertFalse(validador.validate(new DispositivosController.Solicitud(malo)).isEmpty(), String.valueOf(malo));
    }
}
