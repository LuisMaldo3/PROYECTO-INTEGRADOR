package pe.tecsup.vynk.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.service.OrganizadorDetalleService;
import pe.tecsup.vynk.service.OrganizadorHorarioService;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.repository.InscripcionRepository;

@RestController
@RequestMapping("/api/organizador/actividades")
public class OrganizadorActividadesController {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final OrganizadorDetalleService detalles;
    private final OrganizadorHorarioService horario;

    public OrganizadorActividadesController(OrganizadorAccessService acceso, InscripcionRepository inscripciones, OrganizadorDetalleService detalles, OrganizadorHorarioService horario) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.detalles = detalles;
        this.horario = horario;
    }

    @GetMapping
    public Map<String, Object> misActividades(@AuthenticationPrincipal Jwt jwt) {
        var usuario = acceso.exigirOrganizador(jwt);
        var items = acceso.misActividades(usuario).stream().map(doc -> {
            Map<String, Object> item = new LinkedHashMap<>(acceso.resumen(doc));
            long inscritos = inscripciones.findByActividadId(String.valueOf(doc.get("_id"))).stream()
                    .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).count();
            item.put("inscritosRegistrados", inscritos);
            return horario.enriquecer(item);
        }).toList();
        horario.marcarProxima(items);
        return Map.of("total", items.size(), "actividades", items);
    }

    @GetMapping("/{actividadId}")
    public Map<String, Object> detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        return detalles.detalle(jwt, actividadId);
    }
}
