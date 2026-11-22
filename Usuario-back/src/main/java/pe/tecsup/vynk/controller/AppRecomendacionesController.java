package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.RecomendacionesAppService;

/**
 * US-76: recomendaciones de IA para la app móvil.
 *
 *  GET /api/app/recomendaciones  ->  { generadoEn, vigenteHasta, origen (ia | reglas | sin_actividades), conHorario,
 *                                      aviso, intereses, total, recomendaciones: [ actividad del catálogo + motivo,
 *                                      etiquetas, cabeEnHorario, huecoInicio, huecoFin, modalidadInscripcion,
 *                                      inscripcionDirecta ] }
 *
 * Descartar sigue en POST /api/recomendaciones/{id}/descartar e inscribirse en POST /api/actividades/{id}/inscripcion.
 */
@RestController
@RequestMapping("/api/app/recomendaciones")
public class AppRecomendacionesController {
    private final RecomendacionesAppService servicio;

    public AppRecomendacionesController(RecomendacionesAppService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.generar(jwt == null ? null : jwt.getSubject(), Instant.now());
    }
}
