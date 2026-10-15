package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Roles;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Identidad y autorización CENTRALIZADA: nunca se acepta organizadorId enviado por el cliente. */
@Service
public class OrganizadorAccessService {
    private final MongoTemplate mongo;
    private final UsuarioRepository usuarios;

    public OrganizadorAccessService(MongoTemplate mongo, UsuarioRepository usuarios) {
        this.mongo = mongo;
        this.usuarios = usuarios;
    }

    public Usuario exigirOrganizador(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión");
        }
        Usuario usuario = usuarios.findById(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo() || !Roles.ORGANIZADOR.equalsIgnoreCase(usuario.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un organizador activo puede acceder");
        }
        return usuario;
    }

    public List<Document> misActividades(Usuario organizador) {
        Criteria propietario = new Criteria().orOperator(
                Criteria.where("organizadorId").is(organizador.getId()),
                Criteria.where("organizador.id").is(organizador.getId()));
        List<Document> docs = mongo.find(new Query(propietario), Document.class, "actividades");
        docs.sort(Comparator.comparing((Document d) -> texto(d.get("fecha")))
                .thenComparing(d -> texto(d.get("horaInicio")))
                .thenComparing(d -> texto(d.get("nombre"))));
        return docs;
    }

    public Document exigirActividadPropia(Usuario organizador, String actividadId) {
        if (actividadId == null || !actividadId.matches("[a-fA-F0-9]{24}|[a-zA-Z0-9_-]{1,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de actividad inválido");
        }
        Object id = ObjectId.isValid(actividadId) ? new ObjectId(actividadId) : actividadId;
        Document doc = mongo.findOne(new Query(Criteria.where("_id").is(id)), Document.class, "actividades");
        if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Actividad no encontrada");
        Object propietario = doc.get("organizadorId");
        if (propietario == null && doc.get("organizador") instanceof Map<?, ?> datos) {
            propietario = datos.get("id");
        }
        if (!organizador.getId().equals(texto(propietario))) {
            // No filtrar datos de actividades ajenas ni revelar si existen.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Actividad no encontrada");
        }
        return doc;
    }

    public Map<String, Object> resumen(Document doc) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", texto(doc.get("_id")));
        for (String campo : List.of("nombre", "descripcion", "fecha", "horaInicio", "horaFin", "estado", "categoriaId", "ubicacionId", "maximoParticipantes")) {
            a.put(campo, doc.get(campo));
        }
        a.put("participantesInscritos", doc.get("participantesInscritos") instanceof Number n ? n.intValue() : 0);
        return a;
    }

    private static String texto(Object o) { return o == null ? "" : String.valueOf(o); }
}
