package pe.tecsup.vynk.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

/** El organizador propone posiciones; únicamente Django / administrador las publica. */
@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/resultados")
public class OrganizadorResultadosController {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final MongoTemplate mongo;

    public OrganizadorResultadosController(OrganizadorAccessService acceso,
            InscripcionRepository inscripciones, MongoTemplate mongo) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.mongo = mongo;
    }

    private Document verificar(Jwt jwt, String actividadId) {
        var organizador = acceso.exigirOrganizador(jwt);
        var actividad = acceso.exigirActividadAsignada(organizador, actividadId);
        if (!"competencia".equals(String.valueOf(actividad.get("tipoActividad")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta actividad no es una competencia");
        }
        return actividad;
    }

    private List<Map<String, Object>> participantes(String actividadId) {
        List<Map<String, Object>> listado = new ArrayList<>();
        List<Inscripcion> confirmadas = inscripciones.findByActividadId(actividadId).stream()
                .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).toList();
        Map<String, Inscripcion> porAlumno = new HashMap<>();
        for (Inscripcion i : confirmadas) porAlumno.put(i.getEstudianteId(), i);
        for (Inscripcion i : confirmadas) {
            // Los integrantes son inscripciones personales, no competidores adicionales del torneo.
            if ("integrante".equals(i.getRolEquipo())) continue;
            boolean equipo = i.getNombreEquipo() != null && !i.getNombreEquipo().isBlank();
            if (equipo) {
                if (i.getIntegrantes().size() < 2) continue; // sin equipo competitivo confirmado
                boolean completos = i.getIntegrantes().stream().allMatch(id -> {
                    Inscripcion integrante = porAlumno.get(id);
                    return integrante != null && i.getId().equals(integrante.getEquipoId())
                            && "inscrita".equals(integrante.getEstado());
                });
                if (!completos) continue;
            }
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("inscripcionId", i.getId());
            p.put("nombre", equipo ? i.getNombreEquipo() : i.getNombreEstudiante());
            p.put("areaTecsup", i.isAreaVerificada() ? i.getAreaTecsup() : null);
            p.put("integrantesConfirmados", equipo ? i.getIntegrantes().size() : 1);
            listado.add(p);
        }
        listado.sort(Comparator.comparing(p -> String.valueOf(p.get("nombre")), String.CASE_INSENSITIVE_ORDER));
        return listado;
    }

    private Map<String, Object> respuesta(Document doc, List<Map<String, Object>> participantes) {
        Map<String, Object> salida = new LinkedHashMap<>();
        salida.put("estado", doc == null ? "sin_resultados" : doc.getOrDefault("estado", "pendiente"));
        salida.put("posiciones", doc == null ? List.of() : doc.getOrDefault("posiciones", List.of()));
        salida.put("participantes", participantes);
        salida.put("puedeEditar", doc == null || !"publicado".equals(doc.getString("estado")));
        salida.put("origen", doc == null ? null : doc.get("origen"));
        salida.put("revisionesAreas", doc == null ? Map.of() : doc.getOrDefault("revisionesAreas", Map.of()));
        return salida;
    }

    @GetMapping
    public Map<String, Object> consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        var organizador = acceso.exigirOrganizador(jwt);
        Document actividad = verificar(jwt, actividadId);
        Map<String, Object> vista = respuesta(mongo.findOne(new Query(Criteria.where("actividadId").is(actividadId)),
                Document.class, "resultados_competencias"), participantes(actividadId));
        boolean principal = acceso.esPrincipal(actividad, organizador);
        vista.put("puedeEnviar", principal);
        vista.put("areaRevisionPermitida", principal ? null : acceso.areaCoordinador(actividad, organizador));
        return vista;
    }

    @PutMapping
    public Map<String, Object> enviar(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String actividadId, @RequestBody Map<String, Object> cuerpo) {
        var organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        if (!"competencia".equals(actividad.getString("tipoActividad"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se registran resultados en competencias");
        }
        if ("cancelada".equals(actividad.getString("estado"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La competencia está cancelada");
        }
        Object valor = cuerpo == null ? null : cuerpo.get("posiciones");
        if (!(valor instanceof List<?> puestos) || puestos.isEmpty() || puestos.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registra entre 1 y 100 posiciones");
        }
        Map<String, Map<String, Object>> activos = new HashMap<>();
        for (var p : participantes(actividadId)) activos.put(String.valueOf(p.get("inscripcionId")), p);
        Set<String> usados = new HashSet<>();
        Set<Integer> numeros = new HashSet<>();
        List<Document> posiciones = new ArrayList<>();
        for (Object raw : puestos) {
            if (!(raw instanceof Map<?, ?> puesto)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Posiciones inválidas");
            }
            String id = String.valueOf(puesto.get("inscripcionId"));
            Object nro = puesto.get("puesto");
            if (!(nro instanceof Integer numero) || numero < 1 || numero > 100 || !numeros.add(numero)
                    || !usados.add(id) || !activos.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada puesto debe ser único y pertenecer a una inscripción activa");
            }
            Object rawPuntos = puesto.get("puntos");
            // Los puntos son una propuesta del encargado. Django debe validarlos antes de publicar.
            int puntos = 0;
            if (rawPuntos != null) {
                if (!(rawPuntos instanceof Integer cantidad) || cantidad < 0 || cantidad > 10000)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Puntos inválidos (0 a 10000)");
                puntos = (Integer) rawPuntos;
            } else if (actividad.get("puntosPorPuesto") instanceof List<?> tabla && numero <= tabla.size()
                    && tabla.get(numero - 1) instanceof Number numeroPuntos) {
                puntos = numeroPuntos.intValue();
            }
            String evidencia = validarEvidencia(puesto.get("evidencia"));
            Map<String, Object> participante = activos.get(id);
            posiciones.add(new Document("inscripcionId", id).append("puesto", numero)
                    .append("evidencia", evidencia)
                    .append("puntos", puntos)
                    .append("nombre", participante.get("nombre"))
                    .append("areaTecsup", participante.get("areaTecsup")));
        }
        posiciones.sort(Comparator.comparingInt(p -> p.getInteger("puesto")));
        // Índice compartido con Django para impedir dos documentos de resultados por competencia.
        mongo.indexOps("resultados_competencias").ensureIndex(new Index().on("actividadId", org.springframework.data.domain.Sort.Direction.ASC).unique());
        Query filtro = new Query(new Criteria().andOperator(Criteria.where("actividadId").is(actividadId),
                Criteria.where("estado").ne("publicado")));
        Update cambios = new Update().set("posiciones", posiciones).set("estado", "pendiente")
                .set("origen", "organizador").set("registradoPor", organizador.getId())
                .set("revisionesAreas", new Document())
                .set("modificadoEn", java.util.Date.from(Instant.now()))
                .setOnInsert("actividadId", actividadId);
        try {
            Document guardado = mongo.findAndModify(filtro, cambios,
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    Document.class, "resultados_competencias");
            if (guardado == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo registrar la propuesta");
            return respuesta(guardado, participantes(actividadId));
        } catch (DuplicateKeyException duplicado) {
            // Si ya existe una versión publicada, no se permite sobrescribirla.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los resultados ya están publicados. Contacta al administrador si necesitas una corrección");
        }
    }


    private static String validarEvidencia(Object raw) {
        if (raw == null || "".equals(raw)) return "";
        if (!(raw instanceof String enlace) || enlace.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La evidencia no es válida");
        }
        enlace = enlace.trim();
        try {
            java.net.URI uri = java.net.URI.create(enlace);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("URL inválida");
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La evidencia debe ser una URL HTTPS válida");
        }
        return enlace;
    }

    /** El coordinador solo revisa el resultado de su propia área; no aprueba ni publica puntos. */
    @org.springframework.web.bind.annotation.PostMapping("/revision-area")
    public Map<String, Object> revisarArea(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String actividadId, @RequestBody Map<String, Object> cuerpo) {
        var usuario = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadAsignada(usuario, actividadId);
        String area = acceso.areaCoordinador(actividad, usuario);
        if (area == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un coordinador de área puede revisar");
        }
        if (cuerpo == null || !(cuerpo.get("estado") instanceof String estado)
                || !("conforme".equals(estado) || "observado".equals(estado))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona Conforme u Observado");
        }
        String observacion = cuerpo.get("observacion") instanceof String nota ? nota.trim() : "";
        if (observacion.length() > 800 || ("observado".equals(estado) && observacion.length() < 10)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Si observas un resultado, explica el motivo (10 a 800 caracteres)");
        }
        Document propuesta = mongo.findOne(new Query(Criteria.where("actividadId").is(actividadId)
                .and("estado").is("pendiente")), Document.class, "resultados_competencias");
        if (propuesta == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No existe una propuesta pendiente de revisión");
        Object raw = propuesta.get("posiciones");
        boolean tienePuestosArea = raw instanceof List<?> lista && lista.stream()
                .filter(Document.class::isInstance).map(Document.class::cast)
                .anyMatch(p -> area.equals(p.getString("areaTecsup")));
        if (!tienePuestosArea) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La propuesta no contiene resultados atribuidos a tu área");
        Document registro = new Document("estado", estado).append("observacion", observacion)
                .append("revisadoPor", usuario.getId())
                .append("fecha", java.util.Date.from(Instant.now()));
        Query filtro = new Query(Criteria.where("actividadId").is(actividadId).and("estado").is("pendiente")
                .and("modificadoEn").is(propuesta.get("modificadoEn")));
        Document guardado = mongo.findAndModify(filtro,
                new Update().set("revisionesAreas." + area, registro),
                FindAndModifyOptions.options().returnNew(true), Document.class, "resultados_competencias");
        if (guardado == null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La propuesta cambió. Actualiza los resultados y vuelve a revisar");
        return consultar(jwt, actividadId);
    }
}
