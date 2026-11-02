package pe.tecsup.vynk.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.ConversacionAsistente;

/** Historial corto en MongoDB. No recibe IDs de usuario ni historiales desde el cliente. */
@Service
public class ConversacionesAsistenteService {
    private static final int MAX_MENSAJES = 24; // 12 intercambios
    private final MongoTemplate mongo;
    public ConversacionesAsistenteService(MongoTemplate mongo) { this.mongo = mongo; }

    public ConversacionAsistente obtener(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) throw new IllegalArgumentException("Usuario sin identificar");
        Query porId = Query.query(Criteria.where("_id").is(usuarioId));
        // setOnInsert no reemplaza una revisión existente; seguro ante aperturas simultáneas.
        mongo.upsert(porId, new Update().setOnInsert("revision", UUID.randomUUID().toString())
                .setOnInsert("mensajes", List.of()), ConversacionAsistente.class);
        return mongo.findById(usuarioId, ConversacionAsistente.class);
    }

    /** Añade una pareja atómicamente, sin resucitar conversaciones borradas en otro cliente. */
    public boolean guardarIntercambio(String usuarioId, String revision,
                                     ConversacionAsistente.Mensaje pregunta,
                                     ConversacionAsistente.Mensaje respuesta) {
        Query enRevision = Query.query(Criteria.where("_id").is(usuarioId).and("revision").is(revision));
        Update guardar = new Update().push("mensajes").slice(-MAX_MENSAJES).each(pregunta, respuesta);
        return mongo.findAndModify(enRevision, guardar, ConversacionAsistente.class) != null;
    }

    /** Sustituye la revisión y vacía el contenido en una operación atómica. */
    public void borrar(String usuarioId) {
        obtener(usuarioId);
        mongo.updateFirst(Query.query(Criteria.where("_id").is(usuarioId)),
                new Update().set("revision", UUID.randomUUID().toString()).set("mensajes", List.of()),
                ConversacionAsistente.class);
    }
}
