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

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    public AgendaController(InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }
    @GetMapping
    public List<Inscripcion> miAgenda(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(jwt.getSubject())
            .or(() -> usuarios.findById(jwt.getSubject()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return inscripciones.findByEstudianteId(usuario.getId()).stream()
            .sorted(Comparator.comparing((Inscripcion i) -> i.getFecha() == null ? "" : i.getFecha())
                .thenComparing(i -> i.getHoraInicio() == null ? "" : i.getHoraInicio()))
            .toList();
    }
}
