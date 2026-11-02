package pe.tecsup.vynk.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

/** US-07/US-31: inscripción real y control de cupos atómico; no acepta IDs de alumno enviados por el cliente. */
@RestController
@RequestMapping("/api/actividades")
public class InscripcionesCompetenciaController {
    private static final Set<String> AREAS = Set.of("Gestión y Producción", "Tecnología Digital",
            "Minería y Procesos Químicos", "Electricidad y Electrónica", "Mecánica");
    private final MongoTemplate mongo;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final SecureRandom random = new SecureRandom();

    public InscripcionesCompetenciaController(MongoTemplate mongo, InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.mongo = mongo; this.inscripciones = inscripciones; this.usuarios = usuarios;
    }
    private Usuario exigirUsuario(Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión");
        Usuario usuario = usuarios.findByCorreo(jwt.getSubject()).or(() -> usuarios.findById(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no registrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta inactiva");
        return usuario;
    }
    private Object clave(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]{1,100}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID inválido");
        return ObjectId.isValid(id) ? new ObjectId(id) : id;
    }
    private Document actividad(String id) {
        Document doc = mongo.findOne(new Query(Criteria.where("_id").is(clave(id))), Document.class, "actividades");
        if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Actividad no encontrada");
        return doc;
    }
    private String texto(Object valor) { return valor == null ? "" : String.valueOf(valor); }
    private boolean equipos(Document doc) { return "competencia".equals(doc.getString("tipoActividad")) && "equipos".equals(doc.getString("modalidad")); }
    private String areaValida(Usuario u) { return u.isAreaVerificada() && AREAS.contains(u.getAreaTecsup()) ? u.getAreaTecsup() : null; }
    private void sinInscripcionActiva(Usuario usuario, String actividadId) {
        if (inscripciones.findByEstudianteIdAndActividadId(usuario.getId(), actividadId)
                .filter(i -> "inscrita".equals(i.getEstado())).isPresent()
                || mongo.exists(new Query(Criteria.where("actividadId").is(actividadId).and("estado").is("inscrita")
                    .and("integrantes").is(usuario.getId())), "inscripciones"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya estás inscrito en esta actividad");
    }
    private void reservar(Document doc) {
        Object max = doc.get("maximoParticipantes");
        if (!(max instanceof Number n) || n.intValue() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La actividad no tiene cupos configurados");
        Criteria disponible = new Criteria().orOperator(Criteria.where("participantesInscritos").lt(n.intValue()),
                Criteria.where("participantesInscritos").exists(false));
        Query query = new Query(new Criteria().andOperator(Criteria.where("_id").is(doc.get("_id")),
                Criteria.where("estado").is("programada"), disponible));
        Document reservado = mongo.findAndModify(query, new Update().inc("participantesInscritos", 1),
                FindAndModifyOptions.options().returnNew(true), Document.class, "actividades");
        if (reservado == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "No quedan cupos disponibles");
    }
    private void liberar(Document doc) {
        mongo.updateFirst(new Query(new Criteria().andOperator(Criteria.where("_id").is(doc.get("_id")),
                Criteria.where("participantesInscritos").gt(0))), new Update().inc("participantesInscritos", -1), "actividades");
    }
    private String hash(String codigo) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(codigo.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private String nuevoCodigo() {
        byte[] bytes = new byte[12]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private Map<String, Object> respuesta(Inscripcion i, boolean equipo, String codigo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("inscripcionId", i.getId()); datos.put("actividadId", i.getActividadId());
        datos.put("estado", i.getEstado()); datos.put("modalidad", equipo ? "equipos" : "individual");
        datos.put("nombreEquipo", i.getNombreEquipo()); datos.put("integrantes", i.getIntegrantes().size());
        datos.put("areaTecsup", i.getAreaTecsup()); datos.put("areaVerificada", i.isAreaVerificada());
        datos.put("equipoId", i.getEquipoId()); datos.put("rolEquipo", i.getRolEquipo());
        if (codigo != null) datos.put("codigoEquipo", codigo); // se muestra una vez al capitán
        return datos;
    }

    @GetMapping("/{actividadId}/mi-inscripcion")
    public Map<String, Object> miEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Usuario u = exigirUsuario(jwt);
        Document doc = actividad(actividadId);
        Inscripcion i = inscripciones.findByEstudianteIdAndActividadId(u.getId(), actividadId)
                .filter(x -> "inscrita".equals(x.getEstado())).orElse(null);
        // Las antiguas listas de integrantes no sustituyen una inscripción personal confirmada.
        if (i == null) return Map.of("inscrito", false);
        boolean grupal = equipos(doc);
        Map<String, Object> detalle = respuesta(i, grupal, null);
        if (grupal) {
            Inscripcion equipo = "integrante".equals(i.getRolEquipo())
                    ? inscripciones.findById(i.getEquipoId()).orElse(null) : i;
            if (equipo == null || !"inscrita".equals(equipo.getEstado())) return Map.of("inscrito", false);
            long confirmados = equipo.getIntegrantes().stream().filter(miembro -> inscripciones
                    .findByEstudianteIdAndActividadId(miembro, actividadId)
                    .filter(insc -> "inscrita".equals(insc.getEstado())
                            && equipo.getId().equals(insc.getEquipoId())).isPresent()).count();
            detalle.put("nombreEquipo", equipo.getNombreEquipo());
            detalle.put("integrantes", equipo.getIntegrantes().size());
            detalle.put("integrantesConfirmados", confirmados);
            detalle.put("equipoRegistrado", true);
            detalle.put("equipoHabilitado", confirmados >= 2 && confirmados == equipo.getIntegrantes().size());
            detalle.put("maxIntegrantesEquipo", doc.get("maxIntegrantesEquipo"));
        }
        return Map.of("inscrito", true, "detalle", detalle,
                "capitan", grupal && ("capitan".equals(i.getRolEquipo()) || i.getCodigoEquipoHash() != null));
    }

    @PostMapping("/{actividadId}/inscripcion")
    public Map<String, Object> inscribir(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Usuario u = exigirUsuario(jwt);
        Document doc = actividad(actividadId);
        if (!"programada".equals(doc.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Inscripción cerrada");
        sinInscripcionActiva(u, actividadId);
        boolean equipo = equipos(doc);
        String nombreEquipo = body == null ? "" : texto(body.get("nombreEquipo")).trim();
        if (equipo && (nombreEquipo.length() < 3 || nombreEquipo.length() > 70))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del equipo debe tener entre 3 y 70 caracteres");
        if (equipo && (!(doc.get("maxIntegrantesEquipo") instanceof Number n) || n.intValue() < 2))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configura al menos dos integrantes por equipo");
        reservar(doc); // contador atómico antes de guardar; si falla la escritura se revierte
        boolean reservaMiembro = false;
        try {
            // MongoDB exige unicidad de _id: un alumno solo puede pertenecer a un equipo por competencia.
            if (equipo) {
                mongo.insert(new Document("_id", actividadId + ":" + u.getId()), "participantes_equipo");
                reservaMiembro = true;
            }
            Inscripcion i = inscripciones.findByEstudianteIdAndActividadId(u.getId(), actividadId).orElseGet(Inscripcion::new);
            // No reactivar si otro hilo consiguió registrar una inscripción activa.
            if ("inscrita".equals(i.getEstado()) && i.getId() != null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya estás inscrito");
            i.setEstudianteId(u.getId()); i.setNombreEstudiante(u.getNombre()); i.setActividadId(actividadId);
            i.setNombreActividad(texto(doc.get("nombre"))); i.setDescripcion(texto(doc.get("descripcion")));
            i.setFecha(texto(doc.get("fecha"))); i.setHoraInicio(texto(doc.get("horaInicio"))); i.setHoraFin(texto(doc.get("horaFin")));
            i.setUbicacion(texto(doc.get("ubicacionId"))); i.setCategoria(texto(doc.get("categoriaId")));
            i.setEstado("inscrita"); i.setNombreEquipo(equipo ? nombreEquipo : null);
            i.setIntegrantes(equipo ? new ArrayList<>(List.of(u.getId())) : new ArrayList<>());
            i.setAreaTecsup(areaValida(u)); i.setAreaVerificada(areaValida(u) != null);
            i.setRolEquipo(equipo ? "capitan" : null);
            if (!equipo) i.setEquipoId(null);
            String codigo = equipo ? nuevoCodigo() : null;
            i.setCodigoEquipoHash(codigo != null ? hash(codigo) : null);
            Inscripcion guardada = inscripciones.save(i);
            if (equipo) {
                guardada.setEquipoId(guardada.getId());
                guardada = inscripciones.save(guardada);
            }
            return respuesta(guardada, equipo, codigo);
        } catch (RuntimeException e) {
            if (reservaMiembro) mongo.remove(new Query(Criteria.where("_id").is(actividadId + ":" + u.getId())), "participantes_equipo");
            liberar(doc);
            throw e;
        }
    }

    @PostMapping("/{actividadId}/unirse-equipo")
    public Map<String, Object> unirse(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                      @RequestBody Map<String, Object> body) {
        Usuario u = exigirUsuario(jwt);
        Document doc = actividad(actividadId);
        if (!equipos(doc)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La competencia no es por equipos");
        if (!"programada".equals(doc.get("estado"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "Competencia cerrada");
        sinInscripcionActiva(u, actividadId);
        String codigo = texto(body.get("codigoEquipo")).trim();
        if (codigo.length() < 10 || codigo.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido");
        int limite = doc.get("maxIntegrantesEquipo") instanceof Number n ? n.intValue() : 0;
        if (limite < 2) throw new ResponseStatusException(HttpStatus.CONFLICT, "Falta configurar el máximo por equipo");
        Inscripcion equipoDestino = mongo.findOne(new Query(Criteria.where("actividadId").is(actividadId)
                .and("estado").is("inscrita").and("codigoEquipoHash").is(hash(codigo))), Inscripcion.class);
        if (equipoDestino == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Código de equipo inexistente");
        if (equipoDestino.isAreaVerificada() && !equipoDestino.getAreaTecsup().equals(areaValida(u)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El equipo pertenece a otra área o tu área aún no está verificada");
        // La clave única de esta colección protege de dos inscripciones concurrentes en distintos equipos.
        try { mongo.insert(new Document("_id", actividadId + ":" + u.getId()), "participantes_equipo"); }
        catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya perteneces a otro equipo de esta competencia"); }
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("actividadId").is(actividadId), Criteria.where("estado").is("inscrita"),
                Criteria.where("codigoEquipoHash").is(hash(codigo)), Criteria.where("integrantes").ne(u.getId()),
                Criteria.where("integrantes." + (limite - 1)).exists(false)));
        boolean agregado = false;
        try {
            Inscripcion actualizado = mongo.findAndModify(query, new Update().addToSet("integrantes", u.getId()),
                    FindAndModifyOptions.options().returnNew(true), Inscripcion.class);
            if (actualizado == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Código inexistente o equipo completo");
            agregado = true;
            // El equipo existe primero; cada integrante debe confirmar personalmente desde su JWT.
            Inscripcion propia = inscripciones.findByEstudianteIdAndActividadId(u.getId(), actividadId)
                    .orElseGet(Inscripcion::new);
            if ("inscrita".equals(propia.getEstado()) && propia.getId() != null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una inscripción activa");
            propia.setEstudianteId(u.getId()); propia.setNombreEstudiante(u.getNombre());
            propia.setActividadId(actividadId); propia.setNombreActividad(texto(doc.get("nombre")));
            propia.setDescripcion(texto(doc.get("descripcion")));
            propia.setFecha(texto(doc.get("fecha"))); propia.setHoraInicio(texto(doc.get("horaInicio")));
            propia.setHoraFin(texto(doc.get("horaFin")));
            propia.setUbicacion(texto(doc.get("ubicacionId"))); propia.setCategoria(texto(doc.get("categoriaId")));
            propia.setEstado("inscrita"); propia.setNombreEquipo(actualizado.getNombreEquipo());
            propia.setEquipoId(actualizado.getId()); propia.setRolEquipo("integrante");
            propia.setCodigoEquipoHash(null); propia.setIntegrantes(new ArrayList<>());
            propia.setAreaTecsup(areaValida(u)); propia.setAreaVerificada(areaValida(u) != null);
            Inscripcion registrada = inscripciones.save(propia);
            return respuesta(registrada, true, null);
        } catch (RuntimeException e) {
            if (agregado) mongo.updateFirst(new Query(Criteria.where("_id").is(equipoDestino.getId())),
                    new Update().pull("integrantes", u.getId()), Inscripcion.class);
            mongo.remove(new Query(Criteria.where("_id").is(actividadId + ":" + u.getId())), "participantes_equipo");
            throw e;
        }
    }

    @DeleteMapping("/{actividadId}/inscripcion")
    public Map<String, Object> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Usuario u = exigirUsuario(jwt);
        Document doc = actividad(actividadId);
        Inscripcion propia = inscripciones.findByEstudianteIdAndActividadId(u.getId(), actividadId)
                .filter(i -> "inscrita".equals(i.getEstado())).orElse(null);
        if (propia == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una inscripción personal activa");
        // Integrante: cancelar solo SU confirmación y quitarlo del equipo; no liberar una plaza de equipo.
        if (equipos(doc) && "integrante".equals(propia.getRolEquipo())) {
            var anulado = mongo.updateFirst(new Query(Criteria.where("_id").is(propia.getId())
                    .and("estado").is("inscrita")), new Update().set("estado", "cancelada"), Inscripcion.class);
            if (anulado.getModifiedCount() == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripción ya cambió");
            mongo.updateFirst(new Query(Criteria.where("_id").is(propia.getEquipoId())),
                    new Update().pull("integrantes", u.getId()), Inscripcion.class);
            mongo.remove(new Query(Criteria.where("_id").is(actividadId + ":" + u.getId())), "participantes_equipo");
            return Map.of("inscrito", false);
        }
        var actualizado = mongo.updateFirst(new Query(Criteria.where("_id").is(propia.getId())
                .and("estado").is("inscrita")), new Update().set("estado", "cancelada"), Inscripcion.class);
        if (actualizado.getModifiedCount() == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripción ya cambió");
        liberar(doc);
        if (equipos(doc)) {
            // Al cancelar el capitán, TODAS las inscripciones personales vinculadas quedan canceladas.
            mongo.updateMulti(new Query(Criteria.where("equipoId").is(propia.getId())
                    .and("estado").is("inscrita")), new Update().set("estado", "cancelada"), Inscripcion.class);
            for (String miembro : propia.getIntegrantes())
                mongo.remove(new Query(Criteria.where("_id").is(actividadId + ":" + miembro)), "participantes_equipo");
        }
        return Map.of("inscrito", false);
    }
}
