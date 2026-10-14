package pe.tecsup.vynk.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.dto.InteresDto;
import pe.tecsup.vynk.dto.InteresesRequest;
import pe.tecsup.vynk.dto.MisInteresesDto;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.service.InteresesService;

import java.util.List;
import java.util.Map;

/** Intereses del estudiante. Todos los endpoints exigen Authorization: Bearer <token>. */
@RestController
@RequestMapping("/api/intereses")
public class InteresesController {

    private final InteresesService intereses;

    public InteresesController(InteresesService intereses) {
        this.intereses = intereses;
    }

    /** Los intereses disponibles: { total, intereses: [{ id, nombre, descripcion }] } */
    @GetMapping
    public Map<String, Object> disponibles() {
        List<InteresDto> lista = intereses.disponibles().stream()
                .map(i -> new InteresDto(i.getId(), i.getNombre(), i.getDescripcion()))
                .toList();
        return Map.of("total", lista.size(), "intereses", lista);
    }

    /** Lo que eligió la persona: { intereses: ["teatro", ...], actualizadoEn } */
    @GetMapping("/mios")
    public MisInteresesDto mios(@AuthenticationPrincipal Jwt jwt) {
        return aDto(intereses.deUsuario(jwt.getSubject()));
    }

    /** Guarda lo que eligió la persona (reemplaza lo anterior). 400 SELECCION_INVALIDA si algo no se puede guardar. */
    @PutMapping("/mios")
    public MisInteresesDto guardar(@AuthenticationPrincipal Jwt jwt, @RequestBody InteresesRequest cuerpo) {
        return aDto(intereses.guardar(jwt.getSubject(), cuerpo == null ? null : cuerpo.intereses()));
    }

    private static MisInteresesDto aDto(InteresesDeUsuario doc) {
        return new MisInteresesDto(doc.getIntereses(), doc.getActualizadoEn() == null ? null : doc.getActualizadoEn().toString());
    }
}
