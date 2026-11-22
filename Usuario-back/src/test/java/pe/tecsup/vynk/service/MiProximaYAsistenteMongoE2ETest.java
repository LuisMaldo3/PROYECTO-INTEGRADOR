package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.*;

/**
 * Mi próxima actividad y sugerencias del asistente contra un MongoDB real. Además envía cada sugerencia al asistente
 * real (sin Gemini) para comprobar que la entiende y responde con datos. Se activa con MONGODB_E2E_URI.
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class MiProximaYAsistenteMongoE2ETest {
    static final Instant AHORA = Instant.parse("2026-11-23T15:30:00Z"); // lunes 23/11, 10:30 en Lima

    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    InteresesDeUsuarioRepository intereses;
    CatalogoController catalogo;
    MiProximaActividadService miProxima;
    SugerenciasAsistenteService sugerencias;

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_mia_" + System.nanoTime()), convertidor);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        intereses = repos.getRepository(InteresesDeUsuarioRepository.class);
        UsuarioRepository usuarios = repos.getRepository(UsuarioRepository.class);
        Usuario u = new Usuario(); u.setId("u1"); u.setCorreo("ana@tecsup.edu.pe"); usuarios.save(u);
        catalogo = new CatalogoController(mongo, null);
        miProxima = new MiProximaActividadService(catalogo, inscripciones, usuarios);
        var proximas = new ProximasActividadesService(catalogo, inscripciones, usuarios);
        sugerencias = new SugerenciasAsistenteService(miProxima, proximas, new InteresesService(null, intereses, usuarios),
                new DisponibilidadService(repos.getRepository(DisponibilidadRepository.class), usuarios));
        mongo.insert(new Document("_id", "auditorio").append("nombre", "Auditorio").append("pabellon", "Pabellón A")
                .append("referencia", "Frente a la biblioteca"), "ubicaciones");
        mongo.insert(new Document("_id", "polideportivo").append("nombre", "Polideportivo"), "ubicaciones");
    }

    @AfterEach void limpiar() { mongo.getDb().drop(); cliente.close(); }

    ObjectId actividad(String nombre, String estado, String fecha, String inicio, String fin, String lugar, String categoria) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", nombre).append("descripcion", nombre).append("estado", estado)
                .append("fecha", fecha).append("horaInicio", inicio).append("horaFin", fin).append("ubicacionId", lugar)
                .append("categoriaId", categoria).append("maximoParticipantes", 30).append("participantesInscritos", 2), "actividades");
        return id;
    }

    void inscribir(ObjectId actividad, String fecha, String inicio, String fin) {
        Inscripcion i = new Inscripcion();
        i.setEstudianteId("u1"); i.setActividadId(actividad.toHexString()); i.setEstado("inscrita");
        i.setFecha(fecha); i.setHoraInicio(inicio); i.setHoraFin(fin);
        inscripciones.save(i);
    }

    @Test void entregaMiProximaActividadConSuUbicacionYLaQueVieneDespues() {
        ObjectId charla = actividad("Charla de robótica", "programada", "2026-11-23", "11:00", "12:00", "auditorio", "charlas-talleres");
        ObjectId futbol = actividad("Torneo de fútbol", "programada", "2026-11-23", "16:00", "17:00", "polideportivo", "deportes");
        ObjectId pasada = actividad("Desayuno", "programada", "2026-11-23", "08:00", "09:00", "auditorio", "otros");
        inscribir(futbol, "2026-11-23", "16:00", "17:00");
        inscribir(charla, "2026-11-23", "11:00", "12:00");
        inscribir(pasada, "2026-11-23", "08:00", "09:00");

        var r = miProxima.obtener("ana@tecsup.edu.pe", AHORA);
        assertEquals(true, r.get("tiene"));
        @SuppressWarnings("unchecked") var a = (Map<String, Object>) r.get("actividad");
        assertEquals("Charla de robótica", a.get("nombre"));
        assertEquals("Auditorio", a.get("ubicacion"));
        assertEquals("Frente a la biblioteca", a.get("ubicacionReferencia"));
        assertEquals(30L, a.get("minutosParaEmpezar"));
        assertEquals(false, a.get("enCurso"));
        @SuppressWarnings("unchecked") var despues = (Map<String, Object>) r.get("despues");
        assertEquals("Torneo de fútbol", despues.get("nombre"));
        assertEquals("Polideportivo", despues.get("ubicacion"));
    }

    @Test void siEstaEnCursoLaMuestraComoAhoraYSaltaLasCanceladas() {
        ObjectId enCurso = actividad("Taller", "programada", "2026-11-23", "10:00", "11:00", "auditorio", "x");
        ObjectId cancelada = actividad("Feria", "cancelada", "2026-11-23", "10:45", "11:30", "auditorio", "x");
        inscribir(enCurso, "2026-11-23", "10:00", "11:00");
        inscribir(cancelada, "2026-11-23", "10:45", "11:30");
        @SuppressWarnings("unchecked") var a = (Map<String, Object>) miProxima.obtener("ana@tecsup.edu.pe", AHORA).get("actividad");
        assertEquals("Taller", a.get("nombre"));
        assertEquals(true, a.get("enCurso"));
        assertEquals(0L, a.get("minutosParaEmpezar"));
    }

    @Test void sinInscripcionesFuturasNoTieneProxima() {
        var r = miProxima.obtener("ana@tecsup.edu.pe", AHORA);
        assertEquals(false, r.get("tiene"));
        assertNull(r.get("actividad"));
        assertNull(r.get("despues"));
    }

    @Test void lasSugerenciasSeArmanConMisDatosYElAsistenteLasEntiende() {
        // Fechas relativas a «hoy» porque el asistente interpreta «mañana» con el reloj real.
        ZonedDateTime hoyLima = ZonedDateTime.now(ZoneId.of("America/Lima"));
        String manana = hoyLima.toLocalDate().plusDays(1).toString();
        Instant ahora = hoyLima.toInstant();
        ObjectId charla = actividad("Charla de robótica", "programada", manana, "11:00", "12:00", "auditorio", "charlas-talleres");
        actividad("Torneo de fútbol deportes", "programada", manana, "16:00", "17:00", "polideportivo", "deportes");
        inscribir(charla, manana, "11:00", "12:00");
        intereses.save(new InteresesDeUsuario("u1", List.of("deportes"), ahora));

        @SuppressWarnings("unchecked") var lista = (List<String>) sugerencias.sugerir("ana@tecsup.edu.pe", ahora).get("sugerencias");
        assertEquals("¿Dónde se realiza Charla de robótica?", lista.get(0));
        assertEquals("¿Qué requisitos tiene Torneo de fútbol deportes?", lista.get(1));
        assertEquals("¿Qué actividades de deportes hay mañana?", lista.get(2));

        var asistente = new AsistenteHorariosService(catalogo, new ObjectMapper(), new UbicacionesActividadesService(mongo));
        var donde = asistente.responder(lista.get(0), List.of());
        assertEquals("ubicacion", donde.tipoConsulta());
        assertFalse(donde.ubicaciones().isEmpty(), donde.respuesta());
        assertEquals("Auditorio", donde.ubicaciones().get(0).get("lugar"));
        var requisitos = asistente.responder(lista.get(1), List.of());
        assertEquals("requisitos", requisitos.tipoConsulta(), requisitos.respuesta());
        var deportes = asistente.responder(lista.get(2), List.of());
        assertFalse(deportes.actividades().isEmpty(), deportes.respuesta());
    }
}
