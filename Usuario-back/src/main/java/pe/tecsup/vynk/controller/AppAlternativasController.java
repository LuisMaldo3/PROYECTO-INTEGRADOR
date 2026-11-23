package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.AlternativasAsistenciaService;

/**
 * US-79: alternativas cuando no puedo asistir a una actividad en la que estoy inscrito.
 *
 *  GET /api/app/alternativas/{actividadId}?motivo=horario|otro
 *    ->  { actividad: {id, nombre, fecha, horaInicio, horaFin, ubicacion, rolEquipo}, motivo, origen (ia | reglas |
 *          sin_alternativas), puedeCambiar, aviso, total, alternativas: [ actividad del catálogo + motivo, etiquetas,
 *          inscripcionDirecta ] }
 *    404 si no está inscrito en esa actividad.
 */
@RestController
@RequestMapping("/api/app/alternativas")
public class AppAlternativasController {
    private final AlternativasAsistenciaService servicio;

    public AppAlternativasController(AlternativasAsistenciaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{actividadId}")
    public Map<String, Object> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                      @RequestParam(defaultValue = "horario") String motivo) {
        return servicio.buscar(jwt == null ? null : jwt.getSubject(), actividadId, motivo, Instant.now());
    }
}
