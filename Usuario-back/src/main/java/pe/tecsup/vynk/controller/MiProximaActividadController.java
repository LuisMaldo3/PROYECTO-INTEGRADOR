package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.MiProximaActividadService;

/**
 * US-74: dónde se realizará mi próxima actividad.
 *
 *  GET /api/mi-proxima  ->  { ahora, tiene, actividad: {...detalle con ubicacionDetalle, empiezaEn, terminaEn,
 *                             enCurso, minutosParaEmpezar} | null, despues: {id, nombre, horaInicio, ubicacion…} | null }
 */
@RestController
@RequestMapping("/api/mi-proxima")
public class MiProximaActividadController {
    private final MiProximaActividadService servicio;

    public MiProximaActividadController(MiProximaActividadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> obtener(@AuthenticationPrincipal Jwt jwt) {
        return servicio.obtener(jwt == null ? null : jwt.getSubject(), Instant.now());
    }
}
