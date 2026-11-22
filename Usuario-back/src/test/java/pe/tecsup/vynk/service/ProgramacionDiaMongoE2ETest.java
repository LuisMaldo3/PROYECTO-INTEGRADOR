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

/** Programación del día y ubicación de las actividades contra un MongoDB real. Se activa con MONGODB_E2E_URI. */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class ProgramacionDiaMongoE2ETest {
    static final Instant AHORA = Instant.parse("2026-11-22T15:30:00Z"); // domingo 22/11, 10:30 en Lima

    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    ProgramacionDiaService servicio;
    CatalogoController catalogo;

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var fabrica = new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_prog_" + System.nanoTime());
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(fabrica, convertidor);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        UsuarioRepository usuarios = repos.getRepository(UsuarioRepository.class);
        Usuario u = new Usuario(); u.setId("u1"); u.setCorreo("ana@tecsup.edu.pe"); usuarios.save(u);
        catalogo = new CatalogoController(mongo, null);
        servicio = new ProgramacionDiaService(catalogo, inscripciones, usuarios);
        // Ubicaciones como las de Django (id de texto), una con ficha completa.
        mongo.insert(new Document("_id", "auditorio").append("nombre", "Auditorio").append("pabellon", "Pabellón A")
                .append("referencia", "Frente a la biblioteca").append("latitud", -12.0445).append("longitud", -76.9529), "ubicaciones");
        mongo.insert(new Document("_id", "patio-central").append("nombre", "Patio central"), "ubicaciones");
        mongo.insert(new Document("_id", "deportes").append("nombre", "Deportes"), "categorias");
    }

    @AfterEach void limpiar() {
        mongo.getDb().drop();
        cliente.close();
    }

    ObjectId actividad(String nombre, String estado, String fecha, String inicio, String fin, String lugar) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", nombre).append("estado", estado).append("fecha", fecha)
                .append("horaInicio", inicio).append("horaFin", fin).append("ubicacionId", lugar).append("categoriaId", "deportes")
                .append("maximoParticipantes", 30).append("participantesInscritos", 5)
                .append("portada", new Document("tipo", "subida").append("archivoId", "foto123")), "actividades");
        return id;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> actividades(Map<String, Object> r) { return (List<Map<String, Object>>) r.get("actividades"); }

    @Test void entregaTodasLasActividadesDelDiaOrdenadasConEstadoInscripcionYUbicacion() {
        ObjectId tarde = actividad("Concierto", "programada", "2026-11-22", "16:00", "17:00", "patio-central");
        ObjectId manana = actividad("Charla", "programada", "2026-11-22", "10:00", "11:00", "auditorio");
        actividad("Feria", "cancelada", "2026-11-22", "12:00", "13:00", "patio-central");
        actividad("Torneo", "finalizada", "2026-11-22", "08:00", "09:00", "patio-central");
        actividad("Otro día", "programada", "2026-11-23", "10:00", "11:00", "auditorio");
        actividad("Borrador", "inactiva", "2026-11-22", "10:00", "11:00", "auditorio");
        Inscripcion i = new Inscripcion();
        i.setEstudianteId("u1"); i.setActividadId(tarde.toHexString()); i.setEstado("inscrita");
        inscripciones.save(i);

        var r = servicio.delDia("ana@tecsup.edu.pe", "2026-11-22", AHORA);
        var lista = actividades(r);
        assertEquals(List.of("Torneo", "Charla", "Feria", "Concierto"), lista.stream().map(a -> a.get("nombre")).toList());
        assertEquals(List.of("finalizada", "en_curso", "cancelada", "proxima"), lista.stream().map(a -> a.get("estadoDia")).toList());
        assertEquals(List.of(false, false, false, true), lista.stream().map(a -> a.get("inscrito")).toList());

        Map<String, Object> charla = lista.get(1);
        assertEquals(manana.toHexString(), charla.get("id"));
        assertEquals("Auditorio", charla.get("ubicacion"));
        assertEquals("Frente a la biblioteca", charla.get("ubicacionReferencia"));
        @SuppressWarnings("unchecked") var ficha = (Map<String, Object>) charla.get("ubicacionDetalle");
        assertEquals("Pabellón A", ficha.get("pabellon"));
        assertEquals(-12.0445, ficha.get("latitud"));
        assertEquals("/api/portadas/archivo/foto123", charla.get("portadaUrl"));
        assertEquals(25, charla.get("cuposDisponibles"));
        assertEquals("Deportes", charla.get("categoria"));
        assertEquals("2026-11-22", r.get("hoy"));
    }

    @Test void entregaLosDiasConActividadesParaCambiarDeDia() {
        actividad("A", "programada", "2026-11-22", "10:00", "11:00", "auditorio");
        actividad("B", "programada", "2026-11-22", "12:00", "13:00", "auditorio");
        actividad("C", "cancelada", "2026-11-24", "10:00", "11:00", "auditorio");
        actividad("D", "inactiva", "2026-11-25", "10:00", "11:00", "auditorio");
        var r = servicio.delDia("ana@tecsup.edu.pe", "2026-11-24", AHORA);
        assertEquals(List.of(Map.of("fecha", "2026-11-22", "total", 2), Map.of("fecha", "2026-11-24", "total", 1)), r.get("dias"));
        assertEquals("2026-11-24", r.get("fecha"));
        assertEquals(1, r.get("total"));
    }

    @Test void sinFechaAbreElDiaMasUtil() {
        actividad("A", "programada", "2026-11-25", "10:00", "11:00", "auditorio");
        assertEquals("2026-11-25", servicio.delDia("ana@tecsup.edu.pe", null, AHORA).get("fecha"));
        actividad("B", "programada", "2026-11-22", "18:00", "19:00", "auditorio");
        assertEquals("2026-11-22", servicio.delDia("ana@tecsup.edu.pe", "", AHORA).get("fecha"));
    }

    @Test void unDiaSinActividadesDevuelveUnaListaVacia() {
        var r = servicio.delDia("ana@tecsup.edu.pe", "2026-12-31", AHORA);
        assertEquals(0, r.get("total"));
        assertTrue(actividades(r).isEmpty());
    }

    @Test void rechazaFechasMalEscritasYUsuariosSinSesion() {
        assertThrows(ResponseStatusException.class, () -> servicio.delDia("ana@tecsup.edu.pe", "22/11/2026", AHORA));
        assertThrows(ResponseStatusException.class, () -> servicio.delDia(null, "2026-11-22", AHORA));
    }

    @Test void elCatalogoYElDetalleTambienTraenLaUbicacionYSuReferencia() {
        ObjectId id = actividad("Charla", "programada", "2026-11-23", "10:00", "11:00", "auditorio");
        var detalle = catalogo.detalle(id.toHexString());
        assertEquals("Auditorio", detalle.get("ubicacion"));
        assertEquals("Frente a la biblioteca", detalle.get("ubicacionReferencia"));
        @SuppressWarnings("unchecked") var lista = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        assertEquals("Auditorio", lista.get(0).get("ubicacion"));
    }

    @Test void unaUbicacionInexistenteQuedaPorConfirmar() {
        ObjectId id = actividad("Charla", "programada", "2026-11-23", "10:00", "11:00", "no-existe");
        var detalle = catalogo.detalle(id.toHexString());
        assertEquals("Ubicación por confirmar", detalle.get("ubicacion"));
        assertEquals("", detalle.get("ubicacionReferencia"));
    }
}
