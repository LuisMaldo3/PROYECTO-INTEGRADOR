package pe.tecsup.vynk.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorCuposService;
import pe.tecsup.vynk.service.OrganizadorEquiposVerificacionService;

/** US-58/US-59: consultas de solo lectura; el aforo es propiedad del administrador. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}")
public class OrganizadorOcupacionEquiposController {
    private final OrganizadorAccessService acceso;
    private final OrganizadorCuposService cupos;
    private final OrganizadorEquiposVerificacionService equipos;

    public OrganizadorOcupacionEquiposController(OrganizadorAccessService acceso,
            OrganizadorCuposService cupos, OrganizadorEquiposVerificacionService equipos) {
        this.acceso = acceso;
        this.cupos = cupos;
        this.equipos = equipos;
    }

    @GetMapping("/ocupacion")
    public Map<String, Object> ocupacion(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Document actividad = acceso.exigirActividadPropia(acceso.exigirOrganizador(jwt), actividadId);
        Map<String, Object> datos = new LinkedHashMap<>(cupos.enriquecer(acceso.resumen(actividad)));
        Object raw = datos.get("maximoParticipantes");
        Integer maximo = raw instanceof Number n ? n.intValue() : null;
        if (maximo == null && raw instanceof String texto && texto.matches("\\d{1,8}")) {
            try { maximo = Integer.valueOf(texto); } catch (NumberFormatException ignorado) { maximo = null; }
        }
        long ocupados = ((Number) datos.get("cuposOcupados")).longValue();
        datos.put("porcentajeOcupacion", maximo != null && maximo > 0
                ? Math.round(100.0 * ocupados / maximo) : null);
        datos.put("excedeLimite", maximo != null && ocupados > maximo);
        return datos;
    }

    @GetMapping("/equipos/verificacion")
    public Map<String, Object> verificarEquipos(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Document actividad = acceso.exigirActividadPropia(acceso.exigirOrganizador(jwt), actividadId);
        if (!"competencia".equals(actividad.getString("tipoActividad"))
                || !"equipos".equals(actividad.getString("modalidad"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La actividad no es una competencia por equipos");
        }
        Object bruto = actividad.get("maxIntegrantesEquipo");
        Integer maxIntegrantes = bruto instanceof Number n ? n.intValue() : null;
        if (maxIntegrantes == null && bruto instanceof String texto && texto.matches("\\d{1,3}")) {
            maxIntegrantes = Integer.valueOf(texto);
        }
        if (maxIntegrantes != null && maxIntegrantes < 2) maxIntegrantes = null;
        Map<String, Object> resultado = new LinkedHashMap<>(equipos.consultar(actividadId, maxIntegrantes));
        resultado.put("actividadId", actividadId);
        return resultado;
    }
}
