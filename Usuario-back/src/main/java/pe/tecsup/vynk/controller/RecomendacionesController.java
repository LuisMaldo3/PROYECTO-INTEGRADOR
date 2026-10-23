package pe.tecsup.vynk.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.service.SugerenciasHuecosService;

/** Recomendaciones sobre disponibilidad confirmada: la IA nunca valida por sí sola un horario. */
@RestController
@RequestMapping("/api/recomendaciones")
public class RecomendacionesController {
    private final SugerenciasHuecosService sugerencias;

    public RecomendacionesController(SugerenciasHuecosService sugerencias) {
        this.sugerencias = sugerencias;
    }

    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para obtener recomendaciones.");
        return sugerencias.generar(jwt.getSubject());
    }
}
