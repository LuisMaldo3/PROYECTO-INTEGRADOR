package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.AgendaInteligenteService;

/**
 * US-77: agenda inteligente para la app móvil.
 *
 *  GET /api/app/agenda-inteligente  ->  { generadoEn, origen, aviso, iaEstado, conHorario,
 *        resumen: { inscritas, sugeridas, enEquipo, equiposPorConfirmar }, avisos: [texto],
 *        dias: [ { fecha, etiqueta, bloques: [ { tipo (inscrita | sugerida | clase), actividadId, nombre, fecha, horaInicio,
 *        horaFin, ubicacion, categoria, enCurso, modalidad (individual | equipo),
 *        equipo: { nombre, rol, integrantes, confirmados, confirmado } | null, motivo?, inscripcionDirecta? } ] } ] }
 */
@RestController
@RequestMapping("/api/app/agenda-inteligente")
public class AgendaInteligenteController {
    private final AgendaInteligenteService servicio;

    public AgendaInteligenteController(AgendaInteligenteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> obtener(@AuthenticationPrincipal Jwt jwt) {
        return servicio.generar(jwt == null ? null : jwt.getSubject(), Instant.now());
    }
}
