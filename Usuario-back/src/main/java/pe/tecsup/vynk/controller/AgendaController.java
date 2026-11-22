package pe.tecsup.vynk.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import pe.tecsup.vynk.service.NombresUbicacion;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final NombresUbicacion ubicaciones;
    public AgendaController(InscripcionRepository inscripciones, UsuarioRepository usuarios, NombresUbicacion ubicaciones) {
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.ubicaciones = ubicaciones;
    }
    @GetMapping
    public List<Inscripcion> miAgenda(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(jwt.getSubject())
            .or(() -> usuarios.findById(jwt.getSubject()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        var todas = new java.util.LinkedHashMap<String, Inscripcion>();
        for (Inscripcion i : inscripciones.findByEstudianteId(usuario.getId()))
            if ("inscrita".equalsIgnoreCase(i.getEstado())) todas.put(i.getId(), i);
        for (Inscripcion i : inscripciones.buscarPorIntegrante(usuario.getId()))
            if ("inscrita".equalsIgnoreCase(i.getEstado())) todas.put(i.getId(), i);
        // La inscripción guarda el identificador del lugar: se responde con su nombre (sin guardarlo).
        Map<String, String> lugares = todas.isEmpty() ? Map.of() : ubicaciones.cargar();
        for (Inscripcion i : todas.values()) i.setUbicacion(NombresUbicacion.nombre(lugares, i.getUbicacion()));
        // Solo inscripciones vigentes: las de actividades canceladas pasan a «cancelada» y salen de la agenda.
        return todas.values().stream()
            .sorted(Comparator.comparing((Inscripcion i) -> i.getFecha() == null ? "" : i.getFecha())
                .thenComparing(i -> i.getHoraInicio() == null ? "" : i.getHoraInicio()))
            .toList();
    }
}
