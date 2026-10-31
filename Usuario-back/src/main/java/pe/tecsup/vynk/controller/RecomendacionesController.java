package pe.tecsup.vynk.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.service.SugerenciasHuecosService;
import pe.tecsup.vynk.service.DescartesRecomendacionesService;

/** Recomendaciones sobre disponibilidad confirmada: la IA nunca valida por sí sola un horario. */
@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionesController {
    private final SugerenciasHuecosService sugerencias;
    private final DescartesRecomendacionesService descartes;

    public RecomendacionesController(SugerenciasHuecosService sugerencias, DescartesRecomendacionesService descartes) {
        this.sugerencias = sugerencias;
        this.descartes = descartes;
    }

    private static String usuario(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para gestionar recomendaciones.");
        return jwt.getSubject();
    }

    /** Marca una actividad como no interesante para esta cuenta. Petición idempotente. */
    @PostMapping("/{actividadId}/descartar")
    public ResponseEntity<Void> descartar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        descartes.descartar(usuario(jwt), actividadId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt) {
        return sugerencias.generar(usuario(jwt));
    }
}
