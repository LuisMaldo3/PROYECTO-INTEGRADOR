package pe.tecsup.vynk.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.PreferenciasNotificacionService;

/**
 * GET /api/notificaciones/preferencias        -> { recomendaciones: true|false }
 * PUT /api/notificaciones/preferencias  { recomendaciones } -> lo mismo, ya guardado
 */
@RestController
@RequestMapping("/api/notificaciones/preferencias")
public class PreferenciasNotificacionController {
    public record Solicitud(@NotNull Boolean recomendaciones) {}

    private final PreferenciasNotificacionService preferencias;

    public PreferenciasNotificacionController(PreferenciasNotificacionService preferencias) {
        this.preferencias = preferencias;
    }

    @GetMapping
    public Map<String, Object> obtener(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("recomendaciones", preferencias.obtener(sujeto(jwt)).isRecomendaciones());
    }

    @PutMapping
    public Map<String, Object> guardar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitud solicitud) {
        return Map.of("recomendaciones",
                preferencias.guardar(sujeto(jwt), solicitud.recomendaciones(), Instant.now()).isRecomendaciones());
    }

    private static String sujeto(Jwt jwt) {
        return jwt == null ? null : jwt.getSubject();
    }
}
