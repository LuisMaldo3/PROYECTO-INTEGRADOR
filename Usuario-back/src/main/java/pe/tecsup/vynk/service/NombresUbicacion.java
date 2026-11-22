package pe.tecsup.vynk.service;

import java.util.HashMap;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Traduce el identificador de una ubicación (lo que guardan actividades e inscripciones) a su nombre,
 * por ejemplo «Cancha 2». Las ubicaciones las administra Django en la colección «ubicaciones».
 */
@Service
public class NombresUbicacion {
    public static final String POR_CONFIRMAR = "Ubicación por confirmar";
    private final MongoTemplate mongo;

    public NombresUbicacion(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Todas las ubicaciones (son pocas): se cargan una vez por ciclo. */
    public Map<String, String> cargar() {
        Map<String, String> nombres = new HashMap<>();
        for (Document u : mongo.find(new Query(), Document.class, "ubicaciones")) {
            Object nombre = u.get("nombre");
            if (nombre != null && !String.valueOf(nombre).isBlank())
                nombres.put(String.valueOf(u.get("_id")), String.valueOf(nombre).strip());
        }
        return nombres;
    }

    /**
     * Nombre de la ubicación. Un identificador que ya no existe queda «por confirmar»; un texto que no es un
     * identificador (inscripciones antiguas guardaban el nombre) se devuelve tal cual.
     */
    public static String nombre(Map<String, String> nombres, String valor) {
        String v = valor == null ? "" : valor.strip();
        if (v.isEmpty() || "null".equals(v)) return POR_CONFIRMAR;
        String encontrado = nombres.get(v);
        if (encontrado != null) return encontrado;
        return ObjectId.isValid(v) ? POR_CONFIRMAR : v;
    }
}
