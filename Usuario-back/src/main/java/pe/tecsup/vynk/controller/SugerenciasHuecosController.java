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

@RestController
@RequestMapping("/api/agenda/huecos")
public class SugerenciasHuecosController {
    private final SugerenciasHuecosService servicio;
    public SugerenciasHuecosController(SugerenciasHuecosService servicio) { this.servicio = servicio; }
    @GetMapping
    public Map<String, Object> sugerir(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return servicio.generar(jwt.getSubject());
    }
}
