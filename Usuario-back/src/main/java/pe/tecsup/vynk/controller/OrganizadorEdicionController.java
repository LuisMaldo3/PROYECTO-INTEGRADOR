package pe.tecsup.vynk.controller;

import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.service.OrganizadorAccessService;
import pe.tecsup.vynk.service.OrganizadorDetalleService;

/** US-48: lista blanca de campos que puede editar el organizador principal. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/edicion")
public class OrganizadorEdicionController {
    private static final Set<String> CAMPOS = Set.of("descripcion", "reglasCompetencia");
    private final OrganizadorAccessService acceso;
    private final OrganizadorDetalleService detalles;
    private final MongoTemplate mongo;

    public OrganizadorEdicionController(OrganizadorAccessService acceso, OrganizadorDetalleService detalles,
            MongoTemplate mongo) {
        this.acceso = acceso;
        this.detalles = detalles;
        this.mongo = mongo;
    }

    @PatchMapping
    public Map<String, Object> guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
            @RequestBody Map<String, Object> cambios) {
        var organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        if ("cancelada".equalsIgnoreCase(actividad.getString("estado"))
                || "finalizada".equalsIgnoreCase(actividad.getString("estado"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes editar una actividad cerrada");
        }
        if (cambios == null || cambios.isEmpty() || !CAMPOS.containsAll(cambios.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo puedes editar la descripción y el reglamento de competencias");
        }
        Update actualizacion = new Update();
        if (cambios.containsKey("descripcion")) {
            String descripcion = validar(cambios.get("descripcion"), 15, 2500, "Descripción");
            actualizacion.set("descripcion", descripcion);
        }
        if (cambios.containsKey("reglasCompetencia")) {
            if (!"competencia".equals(actividad.getString("tipoActividad"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El reglamento solo puede editarse en competencias");
            }
            actualizacion.set("reglasCompetencia", validar(cambios.get("reglasCompetencia"), 0, 3000, "Reglamento"));
        }
        actualizacion.set("ultimaEdicionOrganizador", java.util.Date.from(java.time.Instant.now()));
        actualizacion.set("ultimaEdicionPor", organizador.getId());
        Document resultado = mongo.findAndModify(
                new Query(Criteria.where("_id").is(actividad.get("_id")).and("estado").nin("cancelada", "finalizada")),
                actualizacion, FindAndModifyOptions.options().returnNew(true), Document.class, "actividades");
        if (resultado == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "La actividad cambió o fue cerrada");
        return detalles.detalle(jwt, actividadId);
    }

    static String validar(Object texto, int minimo, int maximo, String campo) {
        if (!(texto instanceof String s)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " inválido");
        s = s.trim();
        if (s.length() < minimo || s.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    campo + ": longitud permitida " + minimo + " a " + maximo + " caracteres");
        }
        return s;
    }
}
