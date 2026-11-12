package pe.tecsup.vynk.config;

import java.util.List;
import org.bson.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Prepara MongoDB al iniciar VYNK, antes de RolesIniciales e InteresesIniciales.
 *
 * <p>Seguro para una base de datos existente: solo crea colecciones ausentes e
 * inserta catalogos fijos con $setOnInsert. No borra, no reemplaza y no inserta
 * actividades, cuentas, inscripciones, comentarios ni resultados ficticios.</p>
 *
 * <p>Spring Boot y Django deben apuntar a LA MISMA base de datos MongoDB.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BaseDatosInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BaseDatosInicializador.class);

    // Incluye colecciones de Spring Boot y las compartidas con Django/PyMongo.
    // No se crea "asistencias": esa funcionalidad fue retirada del backlog.
    static final List<String> COLECCIONES = List.of(
            "usuarios", "roles", "intereses", "intereses_usuario",
            "actividades", "categorias", "areas_tecsup", "ubicaciones", "avisos",
            "inscripciones", "participantes_equipo", "reemplazos_inscripciones",
            "resultados_competencias", "notificaciones", "permisos_usuarios",
            "comentarios_actividades", "reportes_problemas",
            "recomendaciones_descartadas", "conversaciones_asistente",
            "disponibilidades_semanales", "tokens_revocados");

    static final List<String> AREAS = List.of(
            "Gestión y Producción",
            "Tecnología Digital",
            "Minería y Procesos Químicos",
            "Electricidad y Electrónica",
            "Mecánica");

    private static final List<String> IDS_AREAS = List.of(
            "gestion-produccion", "tecnologia-digital", "mineria-procesos-quimicos",
            "electricidad-electronica", "mecanica");

    private final MongoTemplate mongo;
    private final boolean habilitado;

    public BaseDatosInicializador(MongoTemplate mongo,
            @Value("${vynk.mongodb.inicializar:true}") boolean habilitado) {
        this.mongo = mongo;
        this.habilitado = habilitado;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!habilitado) {
            log.warn("Inicialización de MongoDB desactivada (vynk.mongodb.inicializar=false)");
            return;
        }
        // A diferencia del seed de roles anterior, aquí se muestra el fallo y se
        // detiene el inicio si MongoDB no esta disponible; no se anuncia éxito falso.
        mongo.executeCommand(new Document("ping", 1));
        int creadas = 0;
        for (String coleccion : COLECCIONES) {
            if (crearSiFalta(coleccion)) creadas++;
        }
        for (int i = 0; i < AREAS.size(); i++) {
            insertarCatalogoSiFalta("areas_tecsup", IDS_AREAS.get(i), AREAS.get(i), i + 1);
        }
        // Misma definición usada por Django en actividades/preparacion.py.
        insertarCatalogoSiFalta("categorias", "actividad-cultural", "Actividad cultural", 1);
        insertarCatalogoSiFalta("categorias", "concurso-tc-week", "Concurso T&C Week", 2);
        insertarCatalogoSiFalta("categorias", "deportes", "Deportes", 3);
        insertarCatalogoSiFalta("categorias", "musica-canto", "Música y canto", 4);
        insertarCatalogoSiFalta("categorias", "karaduo", "Karadúo", 5);
        insertarCatalogoSiFalta("categorias", "videojuegos", "Videojuegos", 6);
        insertarCatalogoSiFalta("categorias", "gymkana", "Gymkana", 7);
        insertarCatalogoSiFalta("categorias", "charlas-talleres", "Charlas y talleres", 8);

        log.info("MongoDB preparado: {} colecciones nuevas ({} verificadas); 5 areas y 8 categorias garantizadas sin sobrescribir registros. Los roles e intereses se inicializan por sus runners existentes.",
                creadas, COLECCIONES.size());
    }

    private boolean crearSiFalta(String nombre) {
        if (mongo.collectionExists(nombre)) return false;
        try {
            mongo.createCollection(nombre);
            log.info("Colección creada: {}", nombre);
            return true;
        } catch (RuntimeException ex) {
            // Dos instancias pueden intentar crear la misma colección a la vez.
            // Únicamente se tolera si otra instancia ya la creó.
            if (mongo.collectionExists(nombre)) return false;
            throw ex;
        }
    }

    private void insertarCatalogoSiFalta(String coleccion, String id, String nombre, int orden) {
        Query filtro = new Query(Criteria.where("_id").is(id));
        Update solamenteInsercion = new Update()
                .setOnInsert("nombre", nombre)
                .setOnInsert("orden", orden);
        // La consulta por _id y $setOnInsert hacen la operación idempotente.
        mongo.upsert(filtro, solamenteInsercion, coleccion);
    }
}
