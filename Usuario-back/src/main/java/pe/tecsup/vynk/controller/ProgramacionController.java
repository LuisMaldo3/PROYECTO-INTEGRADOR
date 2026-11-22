package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.ProgramacionDiaService;

/**
 * US-70: programación completa de un día.
 *
 *  GET /api/programacion?fecha=2026-11-23  ->  { fecha, hoy, dias: [{fecha, total}], total, actividades: [...] }
 *
 * Sin «fecha» devuelve hoy (o el próximo día con actividades). Cada actividad trae lo mismo que el catálogo
 * (portada, cupos, ubicación y su ficha) más «inscrito» y «estadoDia» (proxima, en_curso, finalizada, cancelada).
 */
@RestController
@RequestMapping("/api/programacion")
public class ProgramacionController {
    private final ProgramacionDiaService programacion;

    public ProgramacionController(ProgramacionDiaService programacion) {
        this.programacion = programacion;
    }

    @GetMapping
    public Map<String, Object> delDia(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String fecha) {
        return programacion.delDia(jwt == null ? null : jwt.getSubject(), fecha, Instant.now());
    }
}
