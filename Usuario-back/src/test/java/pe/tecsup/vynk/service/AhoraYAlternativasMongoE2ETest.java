package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
import pe.tecsup.vynk.model.DisponibilidadSemanal;
import pe.tecsup.vynk.model.DisponibilidadSemanal;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.*;

/**
 * US-78 y US-79 contra un MongoDB real con los servicios reales (sin clave de Gemini: alternativas por reglas).
 * Se activa con MONGODB_E2E_URI.
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class AhoraYAlternativasMongoE2ETest {
    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    DisponibilidadRepository disponibilidades;
    ActividadesAhoraService ahora;
    AlternativasAsistenciaService alternativas;
    LocalDateTime local;
    String hoy, manana;

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_ahora_" + System.nanoTime()), convertidor);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        UsuarioRepository usuarios = repos.getRepository(UsuarioRepository.class);
        Usuario u = new Usuario(); u.setId("u1"); u.setCorreo("u1@tecsup.edu.pe"); usuarios.save(u);
        var catalogo = new CatalogoController(mongo, null);
        ahora = new ActividadesAhoraService(catalogo, inscripciones, usuarios);
        disponibilidades = repos.getRepository(DisponibilidadRepository.class);
        alternativas = new AlternativasAsistenciaService(usuarios, inscripciones, catalogo, new ObjectMapper(),
                new DisponibilidadService(disponibilidades, usuarios));
        mongo.insert(new Document("_id", "auditorio").append("nombre", "Auditorio"), "ubicaciones");
        // Un instante fijo de hoy a las 10:30 (Lima): el catálogo no filtra por el reloj real en estas búsquedas.
        local = LocalDate.now(ZoneId.of("America/Lima")).atTime(10, 30);
        hoy = local.toLocalDate().toString();
        manana = local.toLocalDate().plusDays(1).toString();
    }

    @AfterEach void limpiar() { mongo.getDb().drop(); cliente.close(); }

    Instant instante() { return local.atZone(ZoneId.of("America/Lima")).toInstant(); }

    ObjectId actividad(String nombre, String fecha, String inicio, String fin, String categoria, Object... extra) {
        ObjectId id = new ObjectId();
        Document d = new Document("_id", id).append("nombre", nombre).append("descripcion", nombre).append("estado", "programada")
                .append("fecha", fecha).append("horaInicio", inicio).append("horaFin", fin).append("ubicacionId", "auditorio")
                .append("categoriaId", categoria).append("maximoParticipantes", 30).append("participantesInscritos", 2);
        for (int i = 0; i + 1 < extra.length; i += 2) d.append(String.valueOf(extra[i]), extra[i + 1]);
        mongo.insert(d, "actividades");
        return id;
    }

    Inscripcion inscribir(ObjectId act, String nombre, String fecha, String ini, String fin) {
        Inscripcion i = new Inscripcion();
        i.setEstudianteId("u1"); i.setActividadId(act.toHexString()); i.setNombreActividad(nombre); i.setEstado("inscrita");
        i.setFecha(fecha); i.setHoraInicio(ini); i.setHoraFin(fin);
        return inscripciones.save(i);
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> lista(Map<String, Object> r, String clave) { return (List<Map<String, Object>>) r.get(clave); }

    @Test void queHayAhoraConLugarYComoRespuestaDelAsistente() {
        actividad("Taller de IA", hoy, "10:00", "12:00", "charlas-talleres");
        ObjectId futbol = actividad("Torneo de fútbol", hoy, "11:00", "12:00", "deportes");
        actividad("Concierto", hoy, "18:00", "19:00", "cultural");
        inscribir(futbol, "Torneo de fútbol", hoy, "11:00", "12:00");

        var r = ahora.buscar("u1@tecsup.edu.pe", instante());
        var actividades = lista(r, "actividades");
        assertEquals(List.of("Taller de IA", "Torneo de fútbol"), actividades.stream().map(a -> a.get("nombre")).toList());
        assertEquals("Auditorio", actividades.get(0).get("ubicacion"));
        assertEquals("Torneo de fútbol", actividades.get(0).get("cruzaCon"));
        assertEquals(true, actividades.get(1).get("inscrito"));

        var respuesta = ahora.responder("u1@tecsup.edu.pe", instante());
        assertEquals("ahora", respuesta.tipoConsulta());
        assertEquals(2, respuesta.actividades().size());
        assertTrue(respuesta.respuesta().contains("1 actividad en curso y 1 empieza"));
        // Se puede guardar en la conversación como cualquier otra respuesta.
        assertNotNull(pe.tecsup.vynk.model.ConversacionAsistente.Mensaje.asistente(respuesta));
    }

    @Test void alternativasParaUnaActividadEnLaQueEstoyInscrito() {
        ObjectId taller = actividad("Taller de robótica", manana, "10:00", "12:00", "charlas-talleres");
        actividad("Robótica avanzada", manana, "10:30", "11:30", "charlas-talleres");
        actividad("Robótica móvil", manana, "15:00", "17:00", "charlas-talleres");
        actividad("Feria gastronómica", manana, "13:00", "14:00", "gastronomia");
        inscribir(taller, "Taller de robótica", manana, "10:00", "12:00");

        var r = alternativas.buscar("u1@tecsup.edu.pe", taller.toHexString(), "horario", instante());
        assertEquals("reglas", r.get("origen"));
        assertEquals(true, r.get("puedeCambiar"));
        var lista = lista(r, "alternativas");
        assertEquals(List.of("Robótica móvil", "Feria gastronómica"), lista.stream().map(a -> a.get("nombre")).toList());
        assertEquals(true, lista.get(0).get("inscripcionDirecta"));
        assertTrue(String.valueOf(lista.get(0).get("motivo")).startsWith("Se parece a la que dejas"));
        @SuppressWarnings("unchecked") var act = (Map<String, Object>) r.get("actividad");
        assertEquals("Taller de robótica", act.get("nombre"));

        var otro = alternativas.buscar("u1@tecsup.edu.pe", taller.toHexString(), "otro", instante());
        assertEquals("Robótica avanzada", lista(otro, "alternativas").get(0).get("nombre"));
    }

    @Test void sinInscripcionResponde404YElCapitanNoSeCambiaEnUnToque() {
        ObjectId libre = actividad("Charla", manana, "10:00", "11:00", "charlas-talleres");
        var e = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> alternativas.buscar("u1@tecsup.edu.pe", libre.toHexString(), "horario", instante()));
        assertEquals(404, e.getStatusCode().value());

        ObjectId hack = actividad("Hackathon", manana, "16:00", "19:00", "competencias", "tipoActividad", "competencia", "modalidad", "equipos");
        Inscripcion capitan = inscribir(hack, "Hackathon", manana, "16:00", "19:00");
        capitan.setRolEquipo("capitan");
        inscripciones.save(capitan);
        var r = alternativas.buscar("u1@tecsup.edu.pe", hack.toHexString(), "horario", instante());
        assertEquals(false, r.get("puedeCambiar"));
        assertTrue(String.valueOf(r.get("aviso")).contains("Eres capitán"));
    }

    @Test void conHorarioConfirmadoNoProponeNadaEnHorasDeClase() {
        ObjectId taller = actividad("Taller de robótica", manana, "15:00", "16:00", "charlas-talleres");
        actividad("Robótica en clase", manana, "09:00", "10:00", "charlas-talleres");
        actividad("Robótica de tarde", manana, "17:00", "18:00", "charlas-talleres");
        inscribir(taller, "Taller de robótica", manana, "15:00", "16:00");
        DisponibilidadSemanal d = new DisponibilidadSemanal();
        d.setUsuarioId("u1"); d.setConfirmado(true);
        d.setBloques(new ArrayList<>(List.of(new DisponibilidadSemanal.BloqueOcupado(
                LocalDate.parse(manana).getDayOfWeek().getValue(), "08:00", "13:00", "Clases"))));
        disponibilidades.save(d);
        var r = alternativas.buscar("u1@tecsup.edu.pe", taller.toHexString(), "horario", instante());
        assertEquals(List.of("Robótica de tarde"), lista(r, "alternativas").stream().map(a -> a.get("nombre")).toList());
    }
}
