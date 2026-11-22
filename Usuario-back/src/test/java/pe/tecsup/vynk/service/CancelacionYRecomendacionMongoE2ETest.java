package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.tecsup.vynk.controller.AgendaController;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.AvisoPush;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.RecomendacionDescartada;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.*;

/**
 * Cancelaciones y recomendaciones de punta a punta contra un MongoDB real, con el catálogo, los intereses y los
 * descartes reales. Cancela la actividad como Django (solo su estado). Se activa con MONGODB_E2E_URI.
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class CancelacionYRecomendacionMongoE2ETest {
    // Domingo 22/11/2026, 10:00 en Lima.
    static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");

    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    AvisoPushRepository avisos;
    DispositivoMovilRepository dispositivos;
    RecordatorioActividadRepository recordatorios;
    UsuarioRepository usuarios;
    InteresesDeUsuarioRepository intereses;
    RecomendacionDescartadaRepository descartadas;
    PreferenciasNotificacionService preferencias;
    DetectorCancelaciones detector;
    EnviadorAvisosPush enviador;
    final List<Map<String, String>> enviados = Collections.synchronizedList(new ArrayList<>());
    ObjectId cancha;

    final NotificadorPush push = new NotificadorPush() {
        @Override public boolean disponible() { return true; }
        @Override public Resultado enviar(String token, Map<String, String> datos, Duration vida) {
            enviados.add(datos);
            return Resultado.ENVIADO;
        }
    };

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var fabrica = new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_cancel_" + System.nanoTime());
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(fabrica, convertidor);
        var resolvedor = new MongoPersistentEntityIndexResolver(contexto);
        for (Class<?> tipo : List.of(AvisoPush.class, DispositivoMovil.class, Inscripcion.class, RecordatorioActividad.class, Usuario.class))
            for (var indice : resolvedor.resolveIndexFor(tipo)) mongo.indexOps(tipo).createIndex(indice);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        avisos = repos.getRepository(AvisoPushRepository.class);
        dispositivos = repos.getRepository(DispositivoMovilRepository.class);
        recordatorios = repos.getRepository(RecordatorioActividadRepository.class);
        usuarios = repos.getRepository(UsuarioRepository.class);
        intereses = repos.getRepository(InteresesDeUsuarioRepository.class);
        descartadas = repos.getRepository(RecomendacionDescartadaRepository.class);
        preferencias = new PreferenciasNotificacionService(repos.getRepository(PreferenciasNotificacionRepository.class), usuarios);
        detector = new DetectorCancelaciones(mongo, new NombresUbicacion(mongo));
        enviador = new EnviadorAvisosPush(avisos, inscripciones, dispositivos, preferencias, mongo, push);
        cancha = new ObjectId();
        mongo.insert(new Document("_id", cancha).append("nombre", "Cancha 2"), "ubicaciones");
        for (String u : List.of("u1", "u2")) {
            Usuario usuario = new Usuario();
            usuario.setId(u); usuario.setCorreo(u + "@tecsup.edu.pe"); usuario.setNombre(u);
            usuarios.save(usuario);
        }
    }

    @AfterEach void limpiar() {
        mongo.getDb().drop();
        cliente.close();
    }

    ObjectId actividad(String nombre, String fecha, String inicio, String fin) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", nombre).append("descripcion", "").append("estado", "programada")
                .append("fecha", fecha).append("horaInicio", inicio).append("horaFin", fin).append("ubicacionId", cancha)
                .append("maximoParticipantes", 20).append("participantesInscritos", 1), "actividades");
        return id;
    }

    /** Así cancela el panel de administración (Django): solo cambia el estado de la actividad. */
    void cancelarComoDjango(ObjectId id) {
        mongo.updateFirst(new Query(Criteria.where("_id").is(id)),
                new Update().set("estado", "cancelada").set("canceladaEn", AHORA), "actividades");
    }

    Inscripcion inscribir(String estudiante, ObjectId actividad) {
        Document a = mongo.findById(actividad, Document.class, "actividades");
        Inscripcion i = new Inscripcion();
        i.setEstudianteId(estudiante); i.setActividadId(actividad.toHexString()); i.setNombreActividad(a.getString("nombre"));
        i.setFecha(a.getString("fecha")); i.setHoraInicio(a.getString("horaInicio")); i.setHoraFin(a.getString("horaFin"));
        i.setUbicacion(cancha.toHexString()); i.setEstado("inscrita");
        return inscripciones.save(i);
    }

    void celular(String usuario) {
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId(usuario); d.setToken("token-" + usuario + "-0123456789abcdef");
        dispositivos.save(d);
    }

    List<Inscripcion> agendaDe(String usuario) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(usuario + "@tecsup.edu.pe").build();
        return new AgendaController(inscripciones, usuarios, new NombresUbicacion(mongo)).miAgenda(jwt);
    }

    SelectorRecomendaciones selector() {
        return new SelectorRecomendaciones(new CatalogoController(mongo, null), dispositivos, inscripciones,
                new InteresesService(null, intereses, usuarios), new DescartesRecomendacionesService(descartadas),
                repositorio(DisponibilidadRepository.class), avisos, preferencias, mongo, true);
    }

    <T> T repositorio(Class<T> tipo) {
        return new MongoRepositoryFactory(mongo).getRepository(tipo);
    }

    // ---------- Cancelación ----------

    @Test void alCancelarseNotificaAlInscritoYLaQuitaDeSuAgenda() {
        ObjectId act = actividad("Torneo de fútbol", "2026-11-23", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        assertEquals(1, agendaDe("u1").size());
        assertEquals("Cancha 2", agendaDe("u1").get(0).getUbicacion(), "la agenda muestra el nombre del lugar");
        assertEquals(0, detector.detectar(AHORA), "sin cancelaciones no pasa nada");

        cancelarComoDjango(act);

        assertEquals(1, detector.detectar(AHORA));
        Inscripcion cancelada = inscripciones.findById(i.getId()).orElseThrow();
        assertEquals("cancelada", cancelada.getEstado());
        assertEquals("actividad_cancelada", mongo.findById(i.getId(), Document.class, "inscripciones").getString("motivoCancelacion"));
        assertTrue(agendaDe("u1").isEmpty(), "ya no está en la agenda");

        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals("cancelacion_actividad", enviados.get(0).get("tipo"));
        assertEquals("Se canceló Torneo de fútbol", enviados.get(0).get("titulo"));
        assertEquals("lun 23/11 · 10:00–11:00 · Cancha 2", enviados.get(0).get("detalle"));

        assertEquals(0, detector.detectar(AHORA.plusSeconds(60)), "no se notifica dos veces");
        assertEquals(0, enviador.enviarPendientes(AHORA.plusSeconds(60)));
        assertEquals(1, enviados.size());
    }

    @Test void notificaATodosLosInscritosPeroNoALosQueYaHabianCancelado() {
        ObjectId act = actividad("Torneo de fútbol", "2026-11-23", "10:00", "11:00");
        inscribir("u1", act);
        Inscripcion antes = inscribir("u2", act);
        antes.setEstado("cancelada"); inscripciones.save(antes);
        Inscripcion u3 = inscribir("u3", act);
        celular("u1"); celular("u2"); celular("u3");

        cancelarComoDjango(act);

        assertEquals(2, detector.detectar(AHORA));
        assertEquals(2, enviador.enviarPendientes(AHORA));
        assertNull(mongo.findById(antes.getId(), Document.class, "inscripciones").get("motivoCancelacion"),
                "la que el estudiante canceló antes conserva su motivo");
        assertNotNull(u3.getId());
    }

    @Test void unaActividadCanceladaQueYaPasoSaleDeLaAgendaSinNotificar() {
        ObjectId act = actividad("Charla", "2026-11-22", "08:00", "09:00");
        Inscripcion i = inscribir("u1", act);
        cancelarComoDjango(act);

        assertEquals(0, detector.detectar(AHORA));
        assertEquals("cancelada", inscripciones.findById(i.getId()).orElseThrow().getEstado());
        assertEquals(0, avisos.count());
    }

    @Test void alCancelarseSeFrenaElRecordatorio() {
        ObjectId act = actividad("Torneo de fútbol", "2026-11-22", "10:20", "11:00"); // recordatorio pendiente
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        var programador = new ProgramadorRecordatorios(inscripciones, recordatorios, 30, 48);
        programador.programar(AHORA);
        assertTrue(recordatorios.findByInscripcionId(i.getId()).isPresent());

        cancelarComoDjango(act);
        detector.detectar(AHORA);
        var recordador = new EnviadorRecordatorios(recordatorios, inscripciones, dispositivos, mongo, push, new NombresUbicacion(mongo));
        enviados.clear();
        assertEquals(0, recordador.enviarPendientes(AHORA));
        assertEquals(RecordatorioActividad.CANCELADO, recordatorios.findByInscripcionId(i.getId()).orElseThrow().getEstado());
        assertTrue(enviados.isEmpty(), "no se recuerda una actividad cancelada");
    }

    // ---------- Recomendaciones ----------

    void interesa(String usuario, String... temas) {
        intereses.save(new InteresesDeUsuario(usuario, List.of(temas), AHORA));
    }

    @Test void recomiendaUnaActividadRelacionadaUnaSolaVezAlDia() {
        actividad("Charla de tecnologia e IA", "2026-11-23", "10:00", "11:00");
        actividad("Taller de tecnologia", "2026-11-24", "10:00", "11:00");
        actividad("Recital de poesía", "2026-11-23", "12:00", "13:00");
        interesa("u1", "tecnologia");
        celular("u1");
        var selector = selector();

        assertEquals(1, selector.elegir(AHORA));
        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals("Te puede interesar: Charla de tecnologia e IA", enviados.get(0).get("titulo"));
        assertTrue(enviados.get(0).get("cuerpo").contains("Porque te interesa Tecnología e innovación"));
        assertTrue(enviados.get(0).get("cuerpo").contains("Cancha 2"));

        // Ese mismo día no se vuelve a notificar (aunque pasen más de 15 min).
        assertEquals(0, selector.elegir(AHORA.plusSeconds(3600)));
        // Al día siguiente sí, pero con otra actividad: nunca repite la misma.
        assertEquals(1, selector.elegir(Instant.parse("2026-11-23T14:30:00Z")));
        enviador.enviarPendientes(Instant.parse("2026-11-23T14:30:00Z"));
        assertEquals("Te puede interesar: Taller de tecnologia", enviados.get(1).get("titulo"));
    }

    @Test void noRecomiendaFueraDeHorarioNiSinCelularNiSinIntereses() {
        actividad("Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        interesa("u1", "tecnologia");
        celular("u2"); // u2 tiene celular pero no intereses; u1 tiene intereses pero no celular

        assertEquals(0, selector().elegir(Instant.parse("2026-11-22T12:00:00Z")), "07:00 en Lima: fuera de horario");
        assertEquals(0, selector().elegir(AHORA));
        assertEquals(0, avisos.count());
    }

    @Test void conLasRecomendacionesApagadasNoSeEligeNiSeEnviaNada() {
        actividad("Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        interesa("u1", "tecnologia");
        celular("u1");
        preferencias.guardar("u1@tecsup.edu.pe", false, AHORA);

        assertEquals(0, selector().elegir(AHORA));
        assertFalse(preferencias.obtener("u1@tecsup.edu.pe").isRecomendaciones());

        // Si la apagó después de elegirla, tampoco se envía.
        preferencias.guardar("u1@tecsup.edu.pe", true, AHORA);
        assertEquals(1, selector().elegir(AHORA));
        preferencias.guardar("u1@tecsup.edu.pe", false, AHORA);
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(AvisoPush.DESCARTADO, avisos.findAll().get(0).getEstado());
        assertTrue(enviados.isEmpty());
    }

    @Test void noRecomiendaLoInscritoNiLoDescartadoYSiSeInscribeAntesDeEnviarSeDescarta() {
        ObjectId inscrita = actividad("Charla de tecnologia", "2026-11-23", "10:00", "11:00");
        ObjectId descartada = actividad("Taller de tecnologia", "2026-11-23", "12:00", "13:00");
        ObjectId libre = actividad("Feria de tecnologia", "2026-11-23", "15:00", "16:00");
        inscribir("u1", inscrita);
        descartadas.save(new RecomendacionDescartada("u1", descartada.toHexString()));
        interesa("u1", "tecnologia");
        celular("u1");

        assertEquals(1, selector().elegir(AHORA));
        AvisoPush elegido = avisos.findAll().get(0);
        assertEquals(libre.toHexString(), elegido.getActividadId());

        inscribir("u1", libre); // se inscribe antes de que salga la notificación
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(AvisoPush.DESCARTADO, avisos.findById(elegido.getId()).orElseThrow().getEstado());
    }

    @Test void laPreferenciaPorDefectoEsRecibirRecomendaciones() {
        assertTrue(preferencias.obtener("u1@tecsup.edu.pe").isRecomendaciones());
        assertTrue(preferencias.recomendacionesActivas("u1"));
    }
}
