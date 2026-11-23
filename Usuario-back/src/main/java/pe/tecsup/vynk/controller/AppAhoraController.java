package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.ActividadesAhoraService;

/**
 * US-78: actividades disponibles en este momento (lo mismo que responde el chat a «¿Qué hay ahora?»).
 *
 *  GET /api/app/ahora  ->  { hora, enCurso, pronto, mensaje, siguiente: {id, nombre, horaInicio} | null,
 *                            actividades: [ actividad del catálogo + estadoAhora (en_curso | pronto), minutos,
 *                            detalleAhora, inscrito, cruzaCon, inscripcionDirecta ] }
 */
@RestController
@RequestMapping("/api/app/ahora")
public class AppAhoraController {
    private final ActividadesAhoraService servicio;

    public AppAhoraController(ActividadesAhoraService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> buscar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.buscar(jwt == null ? null : jwt.getSubject(), Instant.now());
    }
}
