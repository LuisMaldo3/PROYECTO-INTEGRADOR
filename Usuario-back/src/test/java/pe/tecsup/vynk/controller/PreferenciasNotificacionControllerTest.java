package pe.tecsup.vynk.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.tecsup.vynk.model.PreferenciasNotificacion;
import pe.tecsup.vynk.service.PreferenciasNotificacionService;

class PreferenciasNotificacionControllerTest {
    final PreferenciasNotificacionService servicio = mock(PreferenciasNotificacionService.class);
    final PreferenciasNotificacionController controller = new PreferenciasNotificacionController(servicio);

    Jwt jwt() {
        return Jwt.withTokenValue("t").header("alg", "none").subject("u1@tecsup.edu.pe").build();
    }

    static PreferenciasNotificacion con(boolean valor) {
        PreferenciasNotificacion p = new PreferenciasNotificacion();
        p.setRecomendaciones(valor);
        return p;
    }

    @Test void devuelveLaPreferenciaDeLaSesion() {
        when(servicio.obtener("u1@tecsup.edu.pe")).thenReturn(con(true));
        assertEquals(true, controller.obtener(jwt()).get("recomendaciones"));
    }

    @Test void guardaLaPreferenciaSoloParaElUsuarioDeLaSesion() {
        when(servicio.guardar(eq("u1@tecsup.edu.pe"), eq(false), any())).thenReturn(con(false));
        assertEquals(false, controller.guardar(jwt(), new PreferenciasNotificacionController.Solicitud(false)).get("recomendaciones"));
        verify(servicio).guardar(eq("u1@tecsup.edu.pe"), eq(false), any());
    }

    @Test void exigeIndicarSiSeQuierenRecomendaciones() {
        var validador = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validador.validate(new PreferenciasNotificacionController.Solicitud(null)).isEmpty());
        assertTrue(validador.validate(new PreferenciasNotificacionController.Solicitud(true)).isEmpty());
    }

    @Test void porDefectoLasRecomendacionesEstanActivas() {
        assertTrue(new PreferenciasNotificacion().isRecomendaciones());
    }
}
