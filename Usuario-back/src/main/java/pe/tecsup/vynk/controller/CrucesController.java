package pe.tecsup.vynk.controller;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.CruceHorarioService;

@RestController
@RequestMapping("/api/agenda/cruces")
public class CrucesController {
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final CruceHorarioService detector;
    public CrucesController(InscripcionRepository inscripciones, UsuarioRepository usuarios, CruceHorarioService detector) {
        this.inscripciones = inscripciones; this.usuarios = usuarios; this.detector = detector;
    }
    @GetMapping
    public List<CruceHorarioService.Cruce> cruces(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(jwt.getSubject()).or(() -> usuarios.findById(jwt.getSubject()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return detector.detectar(inscripciones.findByEstudianteId(usuario.getId()));
    }
}
