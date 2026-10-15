package pe.tecsup.vynk.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.service.AsistenteHorariosService;

/** POST /api/asistente/preguntar. Requiere el mismo JWT que catálogo/agenda. */
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {
    private final AsistenteHorariosService servicio;
    public AsistenteController(AsistenteHorariosService servicio) { this.servicio = servicio; }

    public record Pregunta(String mensaje, List<AsistenteHorariosService.Mensaje> historial) {}

    @PostMapping("/preguntar")
    public AsistenteHorariosService.Respuesta preguntar(@AuthenticationPrincipal Jwt jwt, @RequestBody Pregunta peticion) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para conversar.");
        if (peticion == null || peticion.mensaje() == null || peticion.mensaje().isBlank() || peticion.mensaje().length() > 600)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe una pregunta de 1 a 600 caracteres.");
        // No confiamos en el historial para fechas: el servicio siempre consulta el catálogo real.
        List<AsistenteHorariosService.Mensaje> historial = peticion.historial() == null ? List.of() : peticion.historial();
        if (historial.size() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El historial es demasiado largo.");
        return servicio.responder(peticion.mensaje(), historial);
    }
}
