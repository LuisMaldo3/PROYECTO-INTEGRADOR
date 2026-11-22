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
import pe.tecsup.vynk.service.EquiposAgenda;
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
        // US-77-T06: las propias, sin las de un equipo que ya no está activo (igual que «mi-inscripcion»).
        var sincronizadas = EquiposAgenda.sincronizar(inscripciones.findByEstudianteId(usuario.getId()),
                inscripciones::findById, inscripciones::findByEstudianteIdAndActividadId);
        var todas = unir(sincronizadas.entradas().stream().map(EquiposAgenda.Entrada::inscripcion).toList(),
                inscripciones.buscarPorIntegrante(usuario.getId()),
                sincronizadas.sinEquipo().stream().map(Inscripcion::getActividadId).toList());
        // La inscripción guarda el identificador del lugar: se responde con su nombre (sin guardarlo).
        Map<String, String> lugares = todas.isEmpty() ? Map.of() : ubicaciones.cargar();
        for (Inscripcion i : todas.values()) i.setUbicacion(NombresUbicacion.nombre(lugares, i.getUbicacion()));
        // Solo inscripciones vigentes: las de actividades canceladas pasan a «cancelada» y salen de la agenda.
        return todas.values().stream()
            .sorted(Comparator.comparing((Inscripcion i) -> i.getFecha() == null ? "" : i.getFecha())
                .thenComparing(i -> i.getHoraInicio() == null ? "" : i.getHoraInicio()))
            .toList();
    }

    /**
     * Una sola fila por actividad. Un integrante tiene su propia inscripción y además figura en la del capitán:
     * antes salían las dos (la competencia aparecía repetida). Se muestra la propia; la del equipo solo si el
     * estudiante figura en una lista antigua sin inscripción propia, y nunca la de un equipo del que ya salió.
     */
    static java.util.LinkedHashMap<String, Inscripcion> unir(List<Inscripcion> propias, List<Inscripcion> deEquipo,
                                                              List<String> sinEquipo) {
        var porActividad = new java.util.LinkedHashMap<String, Inscripcion>();
        for (Inscripcion i : propias)
            if ("inscrita".equalsIgnoreCase(i.getEstado())) porActividad.putIfAbsent(String.valueOf(i.getActividadId()), i);
        for (Inscripcion i : deEquipo)
            if ("inscrita".equalsIgnoreCase(i.getEstado()) && !sinEquipo.contains(i.getActividadId()))
                porActividad.putIfAbsent(String.valueOf(i.getActividadId()), i);
        return porActividad;
    }
}
