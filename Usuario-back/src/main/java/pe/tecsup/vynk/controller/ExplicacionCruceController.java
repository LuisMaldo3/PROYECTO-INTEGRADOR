package pe.tecsup.vynk.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.CruceHorarioService;
import pe.tecsup.vynk.service.ExplicacionCruceService;

@RestController
@RequestMapping("/api/agenda/cruces")
public class ExplicacionCruceController {
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final CruceHorarioService detector;
    private final ExplicacionCruceService explicador;
    public ExplicacionCruceController(InscripcionRepository inscripciones, UsuarioRepository usuarios,
            CruceHorarioService detector, ExplicacionCruceService explicador) {
        this.inscripciones = inscripciones; this.usuarios = usuarios;
        this.detector = detector; this.explicador = explicador;
    }
    @GetMapping("/{id}/explicacion")
    public ExplicacionCruceService.Explicacion explicar(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(jwt.getSubject()).or(() -> usuarios.findById(jwt.getSubject()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return detector.detectar(inscripciones.findByEstudianteId(usuario.getId())).stream()
            .filter(c -> c.id().equals(id)).findFirst()
            .map(explicador::explicar)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cruce no encontrado"));
    }
}
