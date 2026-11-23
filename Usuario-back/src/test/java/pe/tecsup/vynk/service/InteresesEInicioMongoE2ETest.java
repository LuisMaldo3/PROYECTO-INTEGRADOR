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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import pe.tecsup.vynk.controller.AvisosPublicosController;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.*;

/** US-80 y US-81 contra un MongoDB real con los servicios reales. Se activa con MONGODB_E2E_URI. */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class InteresesEInicioMongoE2ETest {
    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    InteresesDeUsuarioRepository intereses;
    CatalogoController catalogo;
    InicioAppService inicio;
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
        mongo = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_inicio_" + System.nanoTime()), convertidor);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        intereses = repos.getRepository(InteresesDeUsuarioRepository.class);
        UsuarioRepository usuarios = repos.getRepository(UsuarioRepository.class);
        Usuario u = new Usuario(); u.setId("u1"); u.setCorreo("u1@tecsup.edu.pe"); u.setNombre("Ana Torres"); usuarios.save(u);
        catalogo = new CatalogoController(mongo, null);
        var personalizacion = new PersonalizacionCatalogo(new InteresesService(null, intereses, usuarios), usuarios);
        catalogo.setPersonalizacion(personalizacion);
        inicio = new InicioAppService(usuarios, inscripciones, catalogo, new AvisosPublicosController(mongo), personalizacion);
        mongo.insert(new Document("_id", "auditorio").append("nombre", "Auditorio"), "ubicaciones");
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

    static Jwt token(String sujeto) {
        return Jwt.withTokenValue("t").header("alg", "none").subject(sujeto).build();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> lista(Map<String, Object> r, String clave) { return (List<Map<String, Object>>) r.get(clave); }

    @Test void elCatalogoPoneEnPrimerLugarLoQueMeInteresa() {
        actividad("Charla de cocina", manana, "09:00", "10:00", "gastronomia");
        actividad("Torneo de fútbol", manana, "16:00", "17:00", "deportes");
        actividad("Concierto", manana, "11:00", "12:00", "cultural");
        intereses.save(new InteresesDeUsuario("u1", List.of("deportes"), Instant.now()));

        var r = catalogo.buscar(token("u1@tecsup.edu.pe"), null, null, null, null, null, null, null);
        assertEquals("intereses", r.get("orden"));
        assertEquals(1L, r.get("paraTi"));
        assertEquals(List.of("deportes"), r.get("intereses"));
        var nombres = lista(r, "actividades").stream().map(a -> a.get("nombre")).toList();
        assertEquals(List.of("Torneo de fútbol", "Charla de cocina", "Concierto"), nombres);
        assertEquals("Te interesa: deportes", lista(r, "actividades").get(0).get("motivoParaTi"));

        var porFecha = catalogo.buscar(token("u1@tecsup.edu.pe"), "fecha", null, null, null, null, null, null);
        assertEquals("fecha", porFecha.get("orden"));
        assertEquals("Charla de cocina", lista(porFecha, "actividades").get(0).get("nombre"));
        // Los filtros siguen funcionando junto con el orden.
        var deportes = catalogo.buscar(token("u1@tecsup.edu.pe"), null, null, null, null, null, "deportes", null);
        assertEquals(List.of("Torneo de fútbol"), lista(deportes, "actividades").stream().map(a -> a.get("nombre")).toList());
        // Sin sesión o sin intereses, por fecha como siempre.
        assertEquals("fecha", catalogo.buscar(null, null, null, null, null, null, null, null).get("orden"));
    }

    @Test void elResumenDeInicioTraeMisProximasDestacadasYAvisosConImagen() {
        ObjectId charla = actividad("Charla de IA", manana, "14:00", "15:00", "charlas");
        actividad("Hackathon", manana, "16:00", "19:00", "concursos", "tipoActividad", "competencia", "modalidad", "equipos");
        actividad("Torneo de vóley", manana, "17:00", "18:00", "deportes");
        Inscripcion i = new Inscripcion();
        i.setEstudianteId("u1"); i.setActividadId(charla.toHexString()); i.setEstado("inscrita"); i.setNombreActividad("Charla de IA");
        i.setFecha(manana); i.setHoraInicio("14:00"); i.setHoraFin("15:00");
        inscripciones.save(i);
        intereses.save(new InteresesDeUsuario("u1", List.of("deportes"), Instant.now()));
        mongo.insert(new Document("titulo", "Feria de proyectos").append("mensaje", "El viernes en el patio").append("estado", "publicado")
                .append("publicadoEn", new java.util.Date()).append("portada", new Document("tipo", "subida").append("archivoId", "b".repeat(24))), "avisos");

        var r = inicio.resumen("u1@tecsup.edu.pe", instante());
        @SuppressWarnings("unchecked") var saludo = (Map<String, Object>) r.get("saludo");
        assertEquals("Ana", saludo.get("nombre"));
        assertEquals("Tu próxima actividad es mañana a las 14:00.", saludo.get("mensaje"));
        var proximas = lista(r, "proximas");
        assertEquals(List.of("Charla de IA"), proximas.stream().map(a -> a.get("nombre")).toList());
        assertEquals("Auditorio", proximas.get(0).get("ubicacion"));
        var destacadas = lista(r, "destacadas");
        assertEquals(List.of("Hackathon", "Torneo de vóley"), destacadas.stream().map(a -> a.get("nombre")).toList());
        assertEquals("Para ti", destacadas.get(1).get("destacadaPor"));
        var avisos = lista(r, "avisos");
        assertEquals("/api/portadas/archivo/" + "b".repeat(24), avisos.get(0).get("imagenUrl"));
        assertEquals(true, avisos.get(0).get("nuevo"));
        assertNotNull(r.get("vigenteHasta"));
    }
}
