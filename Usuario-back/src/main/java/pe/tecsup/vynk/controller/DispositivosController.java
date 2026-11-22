package pe.tecsup.vynk.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.DispositivosService;

/** La app registra aquí el token de Firebase del celular para recibir recordatorios de actividades. */
@RestController
@RequestMapping("/api/dispositivos")
public class DispositivosController {
    public record Solicitud(@NotBlank @Pattern(regexp = DispositivosService.PATRON_TOKEN) String token) {}

    private final DispositivosService dispositivos;

    public DispositivosController(DispositivosService dispositivos) {
        this.dispositivos = dispositivos;
    }

    @PutMapping
    public ResponseEntity<Void> registrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitud solicitud) {
        dispositivos.registrar(jwt == null ? null : jwt.getSubject(), solicitud.token(), Instant.now());
        return ResponseEntity.noContent().build();
    }

    /** POST y no DELETE porque HttpURLConnection (la app) no permite enviar cuerpo en un DELETE. */
    @PostMapping("/baja")
    public ResponseEntity<Void> baja(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Solicitud solicitud) {
        dispositivos.quitar(jwt == null ? null : jwt.getSubject(), solicitud.token());
        return ResponseEntity.noContent().build();
    }
}
