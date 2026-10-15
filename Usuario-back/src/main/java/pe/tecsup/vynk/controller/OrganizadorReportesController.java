package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.ReporteProblema;
import pe.tecsup.vynk.repository.ReporteProblemaRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/reportes")
public class OrganizadorReportesController {
    private static final Set<String> CATEGORIAS = Set.of("logistica", "horario", "ubicacion", "materiales", "otro");
    private final OrganizadorAccessService acceso;
    private final ReporteProblemaRepository reportes;
    public OrganizadorReportesController(OrganizadorAccessService acceso, ReporteProblemaRepository reportes) {
        this.acceso = acceso;
        this.reportes = reportes;
    }
    public record Solicitud(String categoria, String descripcion) {}

    @PostMapping
    public ResponseEntity<Map<String, Object>> enviar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                                      @RequestBody Solicitud solicitud) {
        var usuario = acceso.exigirOrganizador(jwt);
        acceso.exigirActividadPropia(usuario, actividadId);
        String categoria = solicitud == null || solicitud.categoria() == null ? "" : solicitud.categoria().trim().toLowerCase();
        String descripcion = solicitud == null || solicitud.descripcion() == null ? "" : solicitud.descripcion().trim();
        if (!CATEGORIAS.contains(categoria)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una categoría válida");
        }
        if (descripcion.length() < 15 || descripcion.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción debe tener entre 15 y 2000 caracteres");
        }
        ReporteProblema reporte = new ReporteProblema();
        reporte.setActividadId(actividadId);
        reporte.setOrganizadorId(usuario.getId());
        reporte.setCategoria(categoria);
        reporte.setDescripcion(descripcion);
        reporte.setCreadoEn(Instant.now());
        reporte = reportes.save(reporte);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", reporte.getId(), "actividadId", actividadId, "estado", reporte.getEstado(),
                "mensaje", "Reporte enviado correctamente", "creadoEn", reporte.getCreadoEn().toString()));
    }
}
