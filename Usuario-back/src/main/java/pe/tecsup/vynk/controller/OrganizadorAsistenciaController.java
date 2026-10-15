package pe.tecsup.vynk.controller;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.AsistenciaService;

@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/asistencia")
public class OrganizadorAsistenciaController {
    private final AsistenciaService asistencia;
    public OrganizadorAsistenciaController(AsistenciaService asistencia) { this.asistencia = asistencia; }
    @PostMapping
    public Map<String, Object> guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                       @RequestBody AsistenciaService.Solicitud solicitud) {
        return asistencia.guardar(jwt, actividadId, solicitud);
    }
}
