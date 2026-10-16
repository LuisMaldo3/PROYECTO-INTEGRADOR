package pe.tecsup.vynk.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Datos exclusivamente del alumno autenticado; el cliente nunca envía un ID de otro usuario. */
@Service
public class ContextoAgendaSugeridaService {
    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final InteresesService intereses;
    private final CatalogoController catalogo;

    public ContextoAgendaSugeridaService(UsuarioRepository usuarios, InscripcionRepository inscripciones,
                                         InteresesService intereses, CatalogoController catalogo) {
        this.usuarios = usuarios;
        this.inscripciones = inscripciones;
        this.intereses = intereses;
        this.catalogo = catalogo;
    }

    public record Contexto(List<Inscripcion> inscritas, List<String> intereses,
                           List<Map<String, Object>> disponibles) {}

    public Contexto reunir(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(sujeto)
                .or(() -> usuarios.findByCorreo(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        List<Inscripcion> actuales = inscripciones.findByEstudianteId(usuario.getId()).stream()
                .filter(i -> i != null && !"cancelada".equalsIgnoreCase(i.getEstado())
                        && !"cancelado".equalsIgnoreCase(i.getEstado()))
                .toList();
        List<String> preferencias = intereses.deUsuario(usuario.getId()).getIntereses();
        if (preferencias == null) preferencias = List.of();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> publicadas = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        List<Map<String, Object>> elegibles = new ArrayList<>();
        if (publicadas != null) for (var a : publicadas) {
            if (a == null || a.get("id") == null) continue;
            String id = String.valueOf(a.get("id"));
            if (actuales.stream().anyMatch(i -> id.equals(i.getActividadId()))) continue;
            Object cupos = a.get("cuposDisponibles");
            if (cupos instanceof Number n && n.intValue() <= 0) continue;
            try {
                LocalDate fecha = LocalDate.parse(String.valueOf(a.get("fecha")));
                LocalTime inicio = LocalTime.parse(String.valueOf(a.get("horaInicio")));
                LocalTime fin = LocalTime.parse(String.valueOf(a.get("horaFin")));
                if (fecha.isBefore(hoy) || !inicio.isBefore(fin)) continue;
            } catch (Exception ignored) { continue; } // No proponer eventos con fecha u hora inventada.
            elegibles.add(a);
        }
        return new Contexto(actuales, List.copyOf(preferencias), List.copyOf(elegibles));
    }
}
