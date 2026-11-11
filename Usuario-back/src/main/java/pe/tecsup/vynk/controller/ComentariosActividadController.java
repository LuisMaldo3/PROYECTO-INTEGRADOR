package pe.tecsup.vynk.controller;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.ComentariosActividadService;

/** Comentarios de participantes autenticados con inscripción activa; nunca expone autores. */
@RestController
@RequestMapping("/api/actividades/{actividadId}/comentarios")
public class ComentariosActividadController {
    private final ComentariosActividadService servicio;

    public ComentariosActividadController(ComentariosActividadService servicio) { this.servicio = servicio; }

    @GetMapping("/mio")
    public Map<String, Object> consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        return servicio.miComentario(jwt, actividadId);
    }

    @PostMapping
    public Map<String, Object> comentar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
            @RequestBody Map<String, String> solicitud) {
        return servicio.guardar(jwt, actividadId, solicitud == null ? null : solicitud.get("texto"));
    }
}
