package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Filtros del catálogo y próximas actividades contra un MongoDB real. Se activa con MONGODB_E2E_URI. */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class FiltrosYProximasMongoE2ETest {
    static final Instant AHORA = Instant.parse("2026-11-23T14:30:00Z"); // lunes 23/11, 09:30 en Lima

    MongoClient cliente;
    MongoTemplate mongo;
    CatalogoController catalogo;
    InscripcionRepository inscripciones;
    ProximasActividadesService proximas;

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_filtros_" + System.nanoTime()), convertidor);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        UsuarioRepository usuarios = repos.getRepository(UsuarioRepository.class);
        Usuario u = new Usuario(); u.setId("u1"); u.setCorreo("ana@tecsup.edu.pe"); usuarios.save(u);
        catalogo = new CatalogoController(mongo, null);
        proximas = new ProximasActividadesService(catalogo, inscripciones, usuarios);
        mongo.insert(new Document("_id", "deportes").append("nombre", "Deportes"), "categorias");
        mongo.insert(new Document("_id", "charlas-talleres").append("nombre", "Charlas y talleres"), "categorias");
    }

    @AfterEach void limpiar() { mongo.getDb().drop(); cliente.close(); }

    ObjectId actividad(String nombre, String estado, String fecha, String inicio, String categoria, String area) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", nombre).append("estado", estado).append("fecha", fecha)
                .append("horaInicio", inicio).append("horaFin", "").append("categoriaId", categoria).append("areaTecsup", area), "actividades");
        return id;
    }

    @SuppressWarnings("unchecked")
    static List<String> nombres(Map<String, Object> r) {
        return ((List<Map<String, Object>>) r.get("actividades")).stream().map(a -> (String) a.get("nombre")).toList();
    }

    @Test void elCatalogoFiltraPorHorarioCategoriaYArea() {
        actividad("Fútbol", "programada", "2026-11-23", "09:00", "deportes", "Mecánica");
        actividad("Charla IA", "programada", "2026-11-23", "15:00", "charlas-talleres", "Tecnología Digital");
        actividad("Vóley", "programada", "2026-11-24", "19:00", "deportes", "Tecnología Digital");
        actividad("Cancelada", "cancelada", "2026-11-23", "09:00", "deportes", "Mecánica");

        assertEquals(3, catalogo.buscar(null, null, null, null, null, null).get("total"), "sin filtros, como antes");
        assertEquals(List.of("Fútbol"), nombres(catalogo.buscar("manana", null, null, null, null, null)));
        assertEquals(List.of("Fútbol", "Vóley"), nombres(catalogo.buscar(null, null, null, null, "deportes", null)));
        assertEquals(List.of("Charla IA", "Vóley"), nombres(catalogo.buscar(null, null, null, null, null, "tecnología digital")));
        assertEquals(List.of("Vóley"), nombres(catalogo.buscar("noche", null, null, null, "deportes", "Tecnología Digital")));
        assertEquals(List.of("Charla IA"), nombres(catalogo.buscar(null, "12:00", "18:00", "2026-11-23", null, null)));
        assertEquals(0, catalogo.buscar(null, null, null, null, "no-existe", null).get("total"));

        var r = catalogo.buscar("tarde", null, null, null, null, null);
        assertEquals("tarde", ((Map<?, ?>) r.get("aplicados")).get("franja"));
        @SuppressWarnings("unchecked") var categorias = (List<Map<String, Object>>) ((Map<String, Object>) r.get("filtros")).get("categorias");
        assertEquals(List.of("Charlas y talleres", "Deportes"), categorias.stream().map(c -> c.get("nombre")).toList(),
                "las opciones salen de todas las actividades, no solo de las filtradas");
        assertThrows(ResponseStatusException.class, () -> catalogo.buscar("madrugada", null, null, null, null, null));
        assertEquals(3, ((List<?>) catalogo.listar().get("actividades")).size(), "el listado interno sigue sin filtros");
    }

    @Test void entregaLasProximasConMinutosYSiEstaInscrito() {
        ObjectId charla = actividad("Charla IA", "programada", "2026-11-23", "10:00", "charlas-talleres", "");
        actividad("Fútbol", "programada", "2026-11-23", "09:00", "deportes", ""); // ya empezó
        actividad("Vóley", "programada", "2026-11-24", "19:00", "deportes", "");
        actividad("Concierto", "programada", "2026-11-23", "16:00", "deportes", "");
        actividad("Cancelada", "cancelada", "2026-11-23", "09:45", "deportes", "");
        Inscripcion i = new Inscripcion(); i.setEstudianteId("u1"); i.setActividadId(charla.toHexString()); i.setEstado("inscrita");
        inscripciones.save(i);

        var r = proximas.proximas("ana@tecsup.edu.pe", null, AHORA);
        assertEquals(List.of("Charla IA", "Concierto", "Vóley"), nombres(r));
        @SuppressWarnings("unchecked") var primera = ((List<Map<String, Object>>) r.get("actividades")).get(0);
        assertEquals(30L, primera.get("minutosParaEmpezar"));
        assertEquals("2026-11-23T15:00:00Z", primera.get("empiezaEn"));
        assertEquals(true, primera.get("inscrito"));
        assertEquals(List.of("Charla IA"), nombres(proximas.proximas("ana@tecsup.edu.pe", 1, AHORA)));
    }

    @Test void validaElLimiteYLaSesion() {
        assertThrows(ResponseStatusException.class, () -> proximas.proximas("ana@tecsup.edu.pe", 0, AHORA));
        assertThrows(ResponseStatusException.class, () -> proximas.proximas("ana@tecsup.edu.pe", 11, AHORA));
        assertThrows(ResponseStatusException.class, () -> proximas.proximas(null, 3, AHORA));
        assertEquals(0, proximas.proximas("ana@tecsup.edu.pe", 3, AHORA).get("total"), "sin actividades, lista vacía");
    }
}
