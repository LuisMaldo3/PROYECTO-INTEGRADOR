package pe.tecsup.vynk.service;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.PreferenciasNotificacion;
import pe.tecsup.vynk.repository.PreferenciasNotificacionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Activar o desactivar las notificaciones de recomendaciones de la cuenta (vale para todos sus celulares). */
@Service
public class PreferenciasNotificacionService {
    private final PreferenciasNotificacionRepository preferencias;
    private final UsuarioRepository usuarios;

    public PreferenciasNotificacionService(PreferenciasNotificacionRepository preferencias, UsuarioRepository usuarios) {
        this.preferencias = preferencias;
        this.usuarios = usuarios;
    }

    /** Sin preferencia guardada, las recomendaciones están activas. */
    public boolean recomendacionesActivas(String usuarioId) {
        return preferencias.findById(usuarioId).map(PreferenciasNotificacion::isRecomendaciones).orElse(true);
    }

    public PreferenciasNotificacion obtener(String sujeto) {
        String id = usuarioId(sujeto);
        return preferencias.findById(id).orElseGet(() -> {
            PreferenciasNotificacion p = new PreferenciasNotificacion();
            p.setUsuarioId(id);
            return p;
        });
    }

    public PreferenciasNotificacion guardar(String sujeto, boolean recomendaciones, Instant ahora) {
        PreferenciasNotificacion p = obtener(sujeto);
        p.setRecomendaciones(recomendaciones);
        p.setActualizadoEn(ahora);
        return preferencias.save(p);
    }

    private String usuarioId(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(sujeto).or(() -> usuarios.findById(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario.getId();
    }
}
