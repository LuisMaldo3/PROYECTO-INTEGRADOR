package pe.tecsup.vynk.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.ComentarioActividad;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.ComentarioActividadRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** US-62/63. El participante comenta su propia inscripción; el organizador consulta agregados. */
@Service
public class ComentariosActividadService {
    private final ComentarioActividadRepository comentarios;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public ComentariosActividadService(ComentarioActividadRepository comentarios,
            InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.comentarios = comentarios;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    private Usuario exigirParticipante(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para comentar");
        }
        Usuario u = usuarios.findById(jwt.getSubject()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no encontrada"));
        if (!u.isActivo() || !"usuario".equalsIgnoreCase(u.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los estudiantes activos pueden comentar");
        }
        return u;
    }

    private boolean inscrito(String actividadId, String estudianteId) {
        return inscripciones.findByEstudianteIdAndActividadId(estudianteId, actividadId)
                .map(i -> "inscrita".equalsIgnoreCase(i.getEstado())).orElse(false);
    }

    public Map<String, Object> miComentario(Jwt jwt, String actividadId) {
        Usuario u = exigirParticipante(jwt);
        boolean permitido = inscrito(actividadId, u.getId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("puedeComentar", permitido);
        resp.put("comentario", permitido ? comentarios.findByActividadIdAndEstudianteId(actividadId, u.getId())
                .map(ComentarioActividad::getTexto).orElse("") : "");
        return resp;
    }

    public Map<String, Object> guardar(Jwt jwt, String actividadId, String texto) {
        Usuario u = exigirParticipante(jwt);
        if (!inscrito(actividadId, u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Debes tener una inscripción activa en esta actividad para comentar");
        }
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.length() < 10 || limpio.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario debe tener entre 10 y 1000 caracteres");
        }
        ComentarioActividad c = comentarios.findByActividadIdAndEstudianteId(actividadId, u.getId())
                .orElseGet(ComentarioActividad::new);
        c.setActividadId(actividadId);
        c.setEstudianteId(u.getId());
        c.setTexto(limpio);
        c.setActualizadoEn(Instant.now());
        comentarios.save(c);
        return Map.of("guardado", true, "comentario", limpio);
    }

    public List<ComentarioActividad> listar(String actividadId) {
        return comentarios.findByActividadIdOrderByActualizadoEnDesc(actividadId);
    }

    public long total(String actividadId) { return comentarios.countByActividadId(actividadId); }
}
