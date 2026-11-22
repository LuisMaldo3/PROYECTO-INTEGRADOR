package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.time.LocalDate;
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
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.*;

/**
 * US-76 y US-77 contra un MongoDB real, con los servicios reales y sin clave de Gemini (propuesta por reglas).
 * Se activa con MONGODB_E2E_URI.
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class RecomendacionesYAgendaAppMongoE2ETest {
    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    InteresesDeUsuarioRepository intereses;
    DisponibilidadRepository disponibilidades;
    RecomendacionesAppService recomendaciones;
    AgendaInteligenteService agenda;
    DescartesRecomendacionesService descartes;
    String manana, pasado;

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_reco_" + System.nanoTime()), convertidor);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        intereses = repos.getRepository(InteresesDeUsuarioRepository.class);
        disponibilidades = repos.getRepository(DisponibilidadRepository.class);
        UsuarioRepository usuarios = repos.getRepository(UsuarioRepository.class);
        for (String id : List.of("u1", "u2")) {
            Usuario u = new Usuario(); u.setId(id); u.setCorreo(id + "@tecsup.edu.pe"); usuarios.save(u);
        }
        var catalogo = new CatalogoController(mongo, null);
        var interesesService = new InteresesService(null, intereses, usuarios);
        var disponibilidad = new DisponibilidadService(disponibilidades, usuarios);
        var contextoAgenda = new ContextoAgendaSugeridaService(usuarios, inscripciones, interesesService, catalogo);
        var gemini = new GeminiAgendaSugeridaService(new ObjectMapper()); // sin clave: reglas
        descartes = new DescartesRecomendacionesService(repos.getRepository(RecomendacionDescartadaRepository.class));
        var agendaSugerida = new AgendaSugeridaService(contextoAgenda, gemini, descartes);
        var huecos = new SugerenciasHuecosService(contextoAgenda, disponibilidad, gemini, descartes);
        recomendaciones = new RecomendacionesAppService(huecos, agendaSugerida, interesesService, disponibilidad);
        agenda = new AgendaInteligenteService(usuarios, inscripciones, agendaSugerida, new NombresUbicacion(mongo), disponibilidad);
        mongo.insert(new Document("_id", "auditorio").append("nombre", "Auditorio"), "ubicaciones");
        mongo.insert(new Document("_id", "cancha").append("nombre", "Cancha 2"), "ubicaciones");
        // Fechas relativas a hoy: el catálogo de sugerencias descarta lo que ya pasó con el reloj real.
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        manana = hoy.plusDays(1).toString();
        pasado = hoy.plusDays(2).toString();
    }

    @AfterEach void limpiar() { mongo.getDb().drop(); cliente.close(); }

    ObjectId actividad(String nombre, String fecha, String inicio, String fin, String lugar, String categoria) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", nombre).append("descripcion", nombre).append("estado", "programada")
                .append("fecha", fecha).append("horaInicio", inicio).append("horaFin", fin).append("ubicacionId", lugar)
                .append("categoriaId", categoria).append("maximoParticipantes", 30).append("participantesInscritos", 27), "actividades");
        return id;
    }

    Inscripcion inscribir(String estudiante, ObjectId act, String nombre, String fecha, String ini, String fin) {
        Inscripcion i = new Inscripcion();
        i.setEstudianteId(estudiante); i.setActividadId(act.toHexString()); i.setNombreActividad(nombre); i.setEstado("inscrita");
        i.setFecha(fecha); i.setHoraInicio(ini); i.setHoraFin(fin); i.setUbicacion("cancha");
        return inscripciones.save(i);
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> lista(Map<String, Object> r, String clave) { return (List<Map<String, Object>>) r.get(clave); }

    @Test void sinHorarioRecomiendaPorInteresesSinCrucesYConTodoParaInscribirse() {
        ObjectId futbol = actividad("Torneo de fútbol", manana, "16:00", "17:00", "cancha", "deportes");
        actividad("Charla de cocina", manana, "10:00", "11:00", "auditorio", "gastronomia");
        ObjectId cruce = actividad("Vóley mixto", manana, "18:00", "19:00", "cancha", "deportes");
        ObjectId taller = actividad("Taller inscrito", manana, "18:30", "19:30", "auditorio", "otros");
        inscribir("u1", taller, "Taller inscrito", manana, "18:30", "19:30");
        intereses.save(new InteresesDeUsuario("u1", List.of("deportes"), Instant.now()));

        var r = recomendaciones.generar("u1@tecsup.edu.pe", Instant.now());
        assertEquals(false, r.get("conHorario"));
        assertEquals("reglas", r.get("origen"));
        assertTrue(String.valueOf(r.get("aviso")).contains("Sube tu horario"));
        var recos = lista(r, "recomendaciones");
        var nombres = recos.stream().map(x -> x.get("nombre")).toList();
        assertEquals("Torneo de fútbol", nombres.get(0), "primero lo que coincide con sus intereses");
        assertFalse(nombres.contains("Vóley mixto"), "se cruza con su inscripción");
        assertFalse(nombres.contains("Taller inscrito"), "ya está inscrito");
        var primera = recos.get(0);
        assertEquals(futbol.toHexString(), primera.get("id"));
        assertEquals("Cancha 2", primera.get("ubicacion"), "trae la ficha del catálogo para el detalle");
        assertEquals(true, primera.get("inscripcionDirecta"));
        assertEquals(List.of("Te interesa: deportes", "Mañana", "Quedan 3 cupos"), primera.get("etiquetas"));
        assertNotNull(r.get("vigenteHasta"));
        assertNotNull(cruce);
    }

    @Test void conHorarioSoloRecomiendaLoQueCabeEnLasHorasLibres() {
        String dia = manana;
        int diaSemana = LocalDate.parse(dia).getDayOfWeek().getValue();
        DisponibilidadSemanal d = new DisponibilidadSemanal();
        d.setUsuarioId("u1"); d.setConfirmado(true);
        d.setBloques(new ArrayList<>(List.of(new DisponibilidadSemanal.BloqueOcupado(diaSemana, "08:00", "13:00", "Clases"))));
        disponibilidades.save(d);
        actividad("En clase", dia, "10:00", "11:00", "auditorio", "deportes");
        actividad("Tarde libre", dia, "15:00", "16:00", "auditorio", "deportes");

        var r = recomendaciones.generar("u1@tecsup.edu.pe", Instant.now());
        assertEquals(true, r.get("conHorario"));
        var recos = lista(r, "recomendaciones");
        assertEquals(List.of("Tarde libre"), recos.stream().map(x -> x.get("nombre")).toList());
        assertEquals(true, recos.get(0).get("cabeEnHorario"));
        assertTrue(((List<?>) recos.get(0).get("etiquetas")).contains("Libre de 13:00 a 21:00"));
    }

    @Test void loDescartadoNoVuelveAAparecer() {
        ObjectId a = actividad("Torneo de ajedrez", manana, "16:00", "17:00", "auditorio", "deportes");
        assertEquals(1, lista(recomendaciones.generar("u1@tecsup.edu.pe", Instant.now()), "recomendaciones").size());
        descartes.descartar("u1@tecsup.edu.pe", a.toHexString()); // se guarda con el sujeto del token
        assertEquals(0, lista(recomendaciones.generar("u1@tecsup.edu.pe", Instant.now()), "recomendaciones").size());
    }

    @Test void laAgendaInteligenteSincronizaElEquipoConfirmadoYSugiereSinCruces() {
        ObjectId robots = actividad("Competencia de robots", pasado, "10:00", "12:00", "cancha", "concursos");
        ObjectId charla = actividad("Charla de IA", manana, "09:00", "10:00", "auditorio", "charlas");
        actividad("Choca con robots", pasado, "11:00", "12:30", "auditorio", "otros");
        actividad("Feria de proyectos", pasado, "15:00", "16:00", "auditorio", "otros");
        inscribir("u2", charla, "Charla de IA", manana, "09:00", "10:00");
        Inscripcion capitan = inscribir("u1", robots, "Competencia de robots", pasado, "10:00", "12:00");
        capitan.setRolEquipo("capitan"); capitan.setEquipoId(capitan.getId()); capitan.setNombreEquipo("Robots");
        capitan.setIntegrantes(new ArrayList<>(List.of("u1", "u2")));
        inscripciones.save(capitan);
        Inscripcion integrante = inscribir("u2", robots, "Competencia de robots", pasado, "10:00", "12:00");
        integrante.setRolEquipo("integrante"); integrante.setEquipoId(capitan.getId()); integrante.setNombreEquipo("Robots");
        inscripciones.save(integrante);

        var r = agenda.generar("u2@tecsup.edu.pe", Instant.now());
        var dias = lista(r, "dias");
        assertEquals(List.of(manana, pasado), dias.stream().map(x -> x.get("fecha")).toList());
        var bloques = lista(dias.get(1), "bloques");
        var nombres = bloques.stream().map(b -> b.get("nombre")).toList();
        assertEquals(List.of("Competencia de robots", "Feria de proyectos"), nombres, "lo que choca con el equipo no se sugiere");
        @SuppressWarnings("unchecked") var equipo = (Map<String, Object>) bloques.get(0).get("equipo");
        assertEquals("Robots", equipo.get("nombre"));
        assertEquals(true, equipo.get("confirmado"));
        assertEquals("integrante", equipo.get("rol"));
        assertEquals("Cancha 2", bloques.get(0).get("ubicacion"));
        assertEquals("sugerida", bloques.get(1).get("tipo"));
        assertEquals(List.of(), r.get("avisos"));

        // El capitán cancela: el integrante deja de tener la competencia en su agenda y puede ocupar ese horario.
        capitan.setEstado("cancelada");
        inscripciones.save(capitan);
        var despues = agenda.generar("u2@tecsup.edu.pe", Instant.now());
        var bloques2 = lista(lista(despues, "dias").get(1), "bloques");
        assertFalse(bloques2.stream().anyMatch(b -> "inscrita".equals(b.get("tipo"))));
        assertTrue(bloques2.stream().anyMatch(b -> "Choca con robots".equals(b.get("nombre"))));
        assertTrue(String.valueOf(despues.get("avisos")).contains("ya no está activo"));
    }

    @Test void conHorarioLaAgendaMuestraLasClasesYNoSugiereNadaEnClase() {
        int diaSemana = LocalDate.parse(manana).getDayOfWeek().getValue();
        DisponibilidadSemanal d = new DisponibilidadSemanal();
        d.setUsuarioId("u1"); d.setConfirmado(true);
        d.setBloques(new ArrayList<>(List.of(new DisponibilidadSemanal.BloqueOcupado(diaSemana, "08:00", "13:00", "Clases de redes"))));
        disponibilidades.save(d);
        actividad("En clase", manana, "09:00", "10:00", "auditorio", "otros");
        actividad("Por la tarde", manana, "15:00", "16:00", "auditorio", "otros");
        var r = agenda.generar("u1@tecsup.edu.pe", Instant.now());
        assertEquals(true, r.get("conHorario"));
        var bloques = lista(lista(r, "dias").get(0), "bloques");
        assertEquals(List.of("clase", "sugerida"), bloques.stream().map(b -> b.get("tipo")).toList());
        assertEquals(List.of("Clases de redes", "Por la tarde"), bloques.stream().map(b -> b.get("nombre")).toList());
    }
}
