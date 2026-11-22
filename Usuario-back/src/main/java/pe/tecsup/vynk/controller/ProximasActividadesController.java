package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.ProximasActividadesService;

/**
 * US-72: qué actividad comienza próximamente.
 *
 *  GET /api/proximas?limite=3  ->  { ahora, total, actividades: [ {...catálogo, empiezaEn, minutosParaEmpezar, inscrito} ] }
 */
@RestController
@RequestMapping("/api/proximas")
public class ProximasActividadesController {
    private final ProximasActividadesService proximas;

    public ProximasActividadesController(ProximasActividadesService proximas) {
        this.proximas = proximas;
    }

    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer limite) {
        return proximas.proximas(jwt == null ? null : jwt.getSubject(), limite, Instant.now());
    }
}
