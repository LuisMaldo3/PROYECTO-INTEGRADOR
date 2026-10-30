package pe.tecsup.vynk.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.ConversacionAsistente;
import pe.tecsup.vynk.service.AsistenteHorariosService;
import pe.tecsup.vynk.service.ConversacionesAsistenteService;

/** Historial sincronizado en MongoDB por sujeto JWT, compartido por React y Android. */
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {
    private final AsistenteHorariosService servicio;
    private final ConversacionesAsistenteService conversaciones;

    public AsistenteController(AsistenteHorariosService servicio, ConversacionesAsistenteService conversaciones) {
        this.servicio = servicio;
        this.conversaciones = conversaciones;
    }

    /** El historial legado del cliente ya no se usa: evita suplantar mensajes anteriores. */
    public record Pregunta(String mensaje, List<AsistenteHorariosService.Mensaje> historial) {}
    public record VistaConversacion(List<ConversacionAsistente.Mensaje> mensajes) {}

    private String sujeto(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para conversar.");
        return jwt.getSubject();
    }

    @GetMapping("/conversacion")
    public VistaConversacion consultar(@AuthenticationPrincipal Jwt jwt) {
        return new VistaConversacion(conversaciones.obtener(sujeto(jwt)).getMensajes());
    }

    @DeleteMapping("/conversacion")
    public void nuevaConversacion(@AuthenticationPrincipal Jwt jwt) {
        conversaciones.borrar(sujeto(jwt));
    }

    @PostMapping("/preguntar")
    public AsistenteHorariosService.Respuesta preguntar(@AuthenticationPrincipal Jwt jwt, @RequestBody Pregunta peticion) {
        String id = sujeto(jwt);
        if (peticion == null || peticion.mensaje() == null || peticion.mensaje().isBlank()
                || peticion.mensaje().length() > 600)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe una pregunta de 1 a 600 caracteres.");
        var conversacion = conversaciones.obtener(id);
        var previos = conversacion.getMensajes();
        var contexto = previos.stream().skip(Math.max(0, previos.size() - 10))
                .map(m -> new AsistenteHorariosService.Mensaje(m.rol(), m.contenido())).toList();
        var respuesta = servicio.responder(peticion.mensaje().trim(), contexto, previos);
        boolean guardado = conversaciones.guardarIntercambio(id, conversacion.getRevision(),
                ConversacionAsistente.Mensaje.usuario(peticion.mensaje().trim()),
                ConversacionAsistente.Mensaje.asistente(respuesta));
        if (!guardado) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La conversación cambió mientras respondía. Actualiza el historial.");
        return respuesta;
    }
}
