package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.InicioAppService;

/**
 * US-81: resumen de la pantalla de inicio de la app.
 *
 *  GET /api/app/inicio  ->  { generadoEn, vigenteHasta,
 *        saludo: { nombre, fecha, mensaje }, resumen: { inscritas, hoy, semana, equiposPorConfirmar },
 *        proximas: [ actividad + empiezaEn, terminaEn, enCurso, minutosParaEmpezar, cuando, equipo? ],
 *        destacadas: [ actividad + tienePortada, paraTi, destacadaPor, cuando ],
 *        avisos: [ { id, titulo, mensaje, publicadoEn, imagenUrl, autor, creditoUrl, nuevo } ] }
 */
@RestController
@RequestMapping("/api/app/inicio")
public class AppInicioController {
    private final InicioAppService servicio;

    public AppInicioController(InicioAppService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> resumen(@AuthenticationPrincipal Jwt jwt) {
        return servicio.resumen(jwt == null ? null : jwt.getSubject(), Instant.now());
    }
}
