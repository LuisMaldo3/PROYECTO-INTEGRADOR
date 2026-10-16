package pe.tecsup.vynk.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.DisponibilidadSemanal;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.service.DetectorHorarioGemini;
import pe.tecsup.vynk.service.DisponibilidadService;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {
    private final DisponibilidadService horarios;
    private final DetectorHorarioGemini detector;
    public record GuardarHorario(List<BloqueOcupado> bloques) {}
    public DisponibilidadController(DisponibilidadService horarios, DetectorHorarioGemini detector) {
        this.horarios = horarios; this.detector = detector;
    }
    private String usuario(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return jwt.getSubject();
    }
    @GetMapping
    public DisponibilidadSemanal obtener(@AuthenticationPrincipal Jwt jwt) { return horarios.obtener(usuario(jwt)); }
    @PutMapping
    public DisponibilidadSemanal guardar(@AuthenticationPrincipal Jwt jwt, @RequestBody GuardarHorario datos) {
        try { return horarios.guardar(usuario(jwt), datos == null ? null : datos.bloques()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    }
    @PostMapping(value = "/interpretar", consumes = "multipart/form-data")
    public DetectorHorarioGemini.Lectura interpretar(@AuthenticationPrincipal Jwt jwt, @RequestParam("imagen") MultipartFile imagen) throws IOException {
        horarios.usuarioId(usuario(jwt));
        if (imagen.getSize() > 5 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 5 MB.");
        return detector.leer(imagen.getBytes(), imagen.getContentType());
    }
}
