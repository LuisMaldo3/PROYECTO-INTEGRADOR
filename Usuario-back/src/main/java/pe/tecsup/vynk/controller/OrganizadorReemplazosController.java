package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

/** US-51: sustitución de un inscrito por otro, sin modificar jamás los cupos del administrador. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/inscritos")
public class OrganizadorReemplazosController {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final MongoTemplate mongo;

    public OrganizadorReemplazosController(OrganizadorAccessService acceso,
            InscripcionRepository inscripciones, UsuarioRepository usuarios, MongoTemplate mongo) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.mongo = mongo;
    }

    public record Solicitud(String inscripcionId, String correoNuevo, String motivo) { }

    @PostMapping("/reemplazar")
    public Map<String, Object> reemplazar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
            @RequestBody Solicitud solicitud) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        if (!"programada".equals(actividad.getString("estado"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede reemplazar antes del cierre de la actividad");
        }
        Document resultados = mongo.findOne(new Query(Criteria.where("actividadId").is(actividadId)),
                Document.class, "resultados_competencias");
        if (resultados != null && !"sin_resultados".equals(resultados.getString("estado"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La competencia ya tiene resultados registrados. Contacta a administración");
        }
        if (solicitud == null || solicitud.inscripcionId() == null || solicitud.inscripcionId().isBlank()
                || solicitud.inscripcionId().length() > 120 || solicitud.correoNuevo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica la inscripción y el correo del reemplazo");
        }
        String correo = solicitud.correoNuevo().trim().toLowerCase(Locale.ROOT);
        if (correo.length() > 254 || !correo.matches("[a-z0-9._%+\\-]+@tecsup\\.edu\\.pe")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utiliza un correo institucional @tecsup.edu.pe");
        }
        String motivo = solicitud.motivo() == null ? "" : solicitud.motivo().trim();
        if (motivo.length() < 5 || motivo.length() > 400) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describe el motivo (5 a 400 caracteres)");
        }
        Inscripcion saliente = inscripciones.findById(solicitud.inscripcionId())
                .filter(i -> actividadId.equals(i.getActividadId()) && "inscrita".equals(i.getEstado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción activa no encontrada"));
        Usuario entrante = usuarios.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El nuevo estudiante debe registrarse primero en VYNK"));
        if (!entrante.isActivo()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La nueva cuenta está inactiva");
        if (saliente.getEstudianteId().equals(entrante.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El reemplazo debe ser otro estudiante");
        }
        // La unicidad por alumno/actividad protege también las reinscripciones previas.
        if (inscripciones.findByEstudianteIdAndActividadId(entrante.getId(), actividadId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estudiante ya posee un registro en esta competencia; consulta con administración");
        }
        boolean porEquipos = "competencia".equals(actividad.getString("tipoActividad"))
                && "equipos".equals(actividad.getString("modalidad"));
        boolean esCapitan = "capitan".equals(saliente.getRolEquipo());
        boolean esMiembro = "integrante".equals(saliente.getRolEquipo());
        if (porEquipos && !esCapitan && !esMiembro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripción no tiene un equipo válido");
        }
        Inscripcion capitan = null;
        if (porEquipos) {
            capitan = esCapitan ? saliente : inscripciones.findById(saliente.getEquipoId())
                    .filter(i -> "inscrita".equals(i.getEstado()) && "capitan".equals(i.getRolEquipo()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "El equipo ya no está activo"));
            if (!capitan.getIntegrantes().contains(saliente.getEstudianteId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no pertenece al equipo activo");
            }
            if (capitan.isAreaVerificada() && (!entrante.isAreaVerificada()
                    || !capitan.getAreaTecsup().equals(entrante.getAreaTecsup()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estudiante de reemplazo debe pertenecer al área verificada del equipo");
            }
            String identificadorNuevo = actividadId + ":" + entrante.getId();
            if (mongo.exists(new Query(Criteria.where("_id").is(identificadorNuevo)), "participantes_equipo")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El nuevo estudiante ya pertenece a otro equipo");
            }
        }

        Document bitacora = new Document("fecha", java.util.Date.from(Instant.now()))
                .append("organizadorId", organizador.getId())
                .append("estudianteRetiradoId", saliente.getEstudianteId())
                .append("estudianteNuevoId", entrante.getId())
                .append("motivo", motivo);
        String idSaliente = saliente.getEstudianteId();
        boolean marcadorNuevo = false;
        boolean integrantesCambiados = false;
        boolean cambioConfirmado = false;
        try {
            if (porEquipos) {
                mongo.insert(new Document("_id", actividadId + ":" + entrante.getId()), "participantes_equipo");
                marcadorNuevo = true;
                if (esMiembro) {
                    Update equipoUpdate = new Update().pull("integrantes", idSaliente)
                            .addToSet("integrantes", entrante.getId());
                    var escrito = mongo.updateFirst(new Query(Criteria.where("_id").is(capitan.getId())
                            .and("estado").is("inscrita").and("integrantes").is(idSaliente)),
                            equipoUpdate, Inscripcion.class);
                    if (escrito.getModifiedCount() != 1) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "El equipo cambió: actualiza la lista");
                    }
                    integrantesCambiados = true;
                }
            }
            Update cambio = new Update()
                    .set("estudianteId", entrante.getId())
                    .set("nombreEstudiante", entrante.getNombre())
                    .set("areaTecsup", entrante.isAreaVerificada() ? entrante.getAreaTecsup() : null)
                    .set("areaVerificada", entrante.isAreaVerificada())
                    .push("historialReemplazos", bitacora);
            if (porEquipos && esCapitan) {
                List<String> miembros = new ArrayList<>(capitan.getIntegrantes());
                miembros.replaceAll(id -> idSaliente.equals(id) ? entrante.getId() : id);
                cambio.set("integrantes", miembros);
            }
            Inscripcion actualizado = mongo.findAndModify(new Query(Criteria.where("_id").is(saliente.getId())
                    .and("estudianteId").is(idSaliente).and("actividadId").is(actividadId)
                    .and("estado").is("inscrita")), cambio,
                    FindAndModifyOptions.options().returnNew(true), Inscripcion.class);
            if (actualizado == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripción cambió; actualiza la lista");
            }
            cambioConfirmado = true;
            if (porEquipos) {
                // El documento de inscripción ya está actualizado. No revertirlo si falla esta limpieza auxiliar.
                try {
                    mongo.remove(new Query(Criteria.where("_id").is(actividadId + ":" + idSaliente)),
                            "participantes_equipo");
                } catch (RuntimeException noLimpieza) {
                    org.slf4j.LoggerFactory.getLogger(OrganizadorReemplazosController.class)
                            .warn("Marcador anterior de equipo pendiente de limpieza para actividad {}", actividadId, noLimpieza);
                }
            }
            // No se modifica maximoParticipantes ni participantesInscritos.
            return Map.of("reemplazado", true, "inscripcionId", actualizado.getId(),
                    "estudianteNuevo", entrante.getNombre() == null ? "Estudiante" : entrante.getNombre(),
                    "cuposModificados", false);
        } catch (DuplicateKeyException duplicado) {
            if (!cambioConfirmado && integrantesCambiados) revertirIntegrante(capitan.getId(), idSaliente, entrante.getId());
            if (!cambioConfirmado && marcadorNuevo) quitarMarcador(actividadId, entrante.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nuevo estudiante ya tiene una inscripción");
        } catch (RuntimeException error) {
            if (!cambioConfirmado && integrantesCambiados) revertirIntegrante(capitan.getId(), idSaliente, entrante.getId());
            if (!cambioConfirmado && marcadorNuevo) quitarMarcador(actividadId, entrante.getId());
            throw error;
        }
    }

    private void revertirIntegrante(String capitanId, String anterior, String nuevo) {
        mongo.updateFirst(new Query(Criteria.where("_id").is(capitanId).and("integrantes").is(nuevo)),
                new Update().pull("integrantes", nuevo).addToSet("integrantes", anterior), Inscripcion.class);
    }
    private void quitarMarcador(String actividadId, String estudianteId) {
        mongo.remove(new Query(Criteria.where("_id").is(actividadId + ":" + estudianteId)), "participantes_equipo");
    }
}
