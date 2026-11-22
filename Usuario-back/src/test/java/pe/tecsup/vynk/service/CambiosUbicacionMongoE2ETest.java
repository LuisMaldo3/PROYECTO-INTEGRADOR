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
import pe.tecsup.vynk.model.AlertaUbicacion;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.repository.AlertaUbicacionRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.RecordatorioActividadRepository;

/**
 * Cambio de ubicación de punta a punta contra un MongoDB real, editando la actividad como lo hace Django
 * (solo «ubicacionId» en la colección «actividades»). Se activa con MONGODB_E2E_URI.
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class CambiosUbicacionMongoE2ETest {
    static final Instant AHORA = Instant.parse("2026-11-22T13:00:00Z"); // 08:00 en Lima

    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    AlertaUbicacionRepository alertas;
    DispositivoMovilRepository dispositivos;
    RecordatorioActividadRepository recordatorios;
    final List<Map<String, String>> enviados = Collections.synchronizedList(new ArrayList<>());
    DetectorCambiosUbicacion detector;
    EnviadorAlertasUbicacion enviador;
    ObjectId cancha, auditorio, laboratorio;

    final NotificadorPush push = new NotificadorPush() {
        @Override public boolean disponible() { return true; }
        @Override public Resultado enviar(String token, Map<String, String> datos, Duration vida) {
            enviados.add(datos);
            return Resultado.ENVIADO;
        }
    };

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var fabrica = new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_ubicacion_" + System.nanoTime());
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(fabrica, convertidor);
        var resolvedor = new MongoPersistentEntityIndexResolver(contexto);
        for (Class<?> tipo : List.of(AlertaUbicacion.class, RecordatorioActividad.class, DispositivoMovil.class, Inscripcion.class))
            for (var indice : resolvedor.resolveIndexFor(tipo)) mongo.indexOps(tipo).createIndex(indice);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        alertas = repos.getRepository(AlertaUbicacionRepository.class);
        dispositivos = repos.getRepository(DispositivoMovilRepository.class);
        recordatorios = repos.getRepository(RecordatorioActividadRepository.class);
        detector = new DetectorCambiosUbicacion(mongo, new NombresUbicacion(mongo));
        enviador = new EnviadorAlertasUbicacion(alertas, inscripciones, dispositivos, mongo, push);
        // Ubicaciones como las crea Django: _id ObjectId y nombre.
        cancha = ubicacion("Cancha 2");
        auditorio = ubicacion("Auditorio principal");
        laboratorio = ubicacion("Laboratorio 3");
    }

    @AfterEach void limpiar() {
        mongo.getDb().drop();
        cliente.close();
    }

    ObjectId ubicacion(String nombre) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", nombre), "ubicaciones");
        return id;
    }

    ObjectId actividad(ObjectId ubicacion, String fecha, String inicio) {
        ObjectId id = new ObjectId();
        mongo.insert(new Document("_id", id).append("nombre", "Torneo de fútbol").append("estado", "programada")
                .append("ubicacionId", ubicacion).append("fecha", fecha).append("horaInicio", inicio).append("horaFin", ""),
                "actividades");
        return id;
    }

    void moverComoDjango(ObjectId actividad, ObjectId ubicacion) {
        mongo.updateFirst(new Query(Criteria.where("_id").is(actividad)), new Update().set("ubicacionId", ubicacion), "actividades");
    }

    /** Copia la ubicación igual que InscripcionesCompetenciaController: el identificador como texto. */
    Inscripcion inscribir(String estudiante, ObjectId actividad) {
        Document a = mongo.findById(actividad, Document.class, "actividades");
        Inscripcion i = new Inscripcion();
        i.setEstudianteId(estudiante); i.setActividadId(actividad.toHexString()); i.setNombreActividad("Torneo de fútbol");
        i.setUbicacion(String.valueOf(a.get("ubicacionId")));
        i.setFecha(a.getString("fecha")); i.setHoraInicio(a.getString("horaInicio")); i.setHoraFin("");
        i.setEstado("inscrita");
        return inscripciones.save(i);
    }

    void celular(String usuario) {
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId(usuario); d.setToken("token-" + usuario + "-0123456789abcdef");
        dispositivos.save(d);
    }

    @Test void detectaElCambioYAvisaConLosNombresDeLaUbicacionAnteriorYLaNueva() {
        ObjectId act = actividad(cancha, "2026-11-22", "10:00");
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        assertEquals(0, detector.detectar(AHORA), "sin cambios no hay alertas");

        moverComoDjango(act, auditorio);

        assertEquals(1, detector.detectar(AHORA));
        assertEquals(auditorio.toHexString(), inscripciones.findById(i.getId()).orElseThrow().getUbicacion());
        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals("Cancha 2", enviados.get(0).get("ubicacionAnterior"));
        assertEquals("Auditorio principal", enviados.get(0).get("ubicacionNueva"));
        assertEquals(act.toHexString(), enviados.get(0).get("actividadId"));

        assertEquals(0, detector.detectar(AHORA.plusSeconds(60)), "el mismo cambio no se detecta dos veces");
        assertEquals(0, enviador.enviarPendientes(AHORA.plusSeconds(60)));
        assertEquals(1, enviados.size());
    }

    @Test void avisaATodosLosInscritosMenosALosQueCancelaron() {
        ObjectId act = actividad(cancha, "2026-11-22", "10:00");
        inscribir("u1", act); inscribir("u2", act);
        Inscripcion cancelada = inscribir("u3", act);
        cancelada.setEstado("cancelada"); inscripciones.save(cancelada);
        celular("u1"); celular("u2"); celular("u3");

        moverComoDjango(act, auditorio);

        assertEquals(2, detector.detectar(AHORA));
        assertEquals(2, enviador.enviarPendientes(AHORA));
        assertTrue(alertas.findByInscripcionId(cancelada.getId()).isEmpty());
    }

    @Test void dosCambiosAntesDeEnviarSeJuntanEnUnaSolaAlerta() {
        ObjectId act = actividad(cancha, "2026-11-22", "10:00");
        Inscripcion i = inscribir("u1", act);
        moverComoDjango(act, auditorio); detector.detectar(AHORA);
        moverComoDjango(act, laboratorio); detector.detectar(AHORA);

        List<AlertaUbicacion> todas = alertas.findByInscripcionId(i.getId());
        assertEquals(1, todas.size());
        assertEquals("Cancha 2", todas.get(0).getUbicacionAnterior());
        assertEquals("Laboratorio 3", todas.get(0).getUbicacionNueva());
    }

    @Test void siVuelveAlLugarDeAntesAntesDeEnviarNoSeAvisa() {
        ObjectId act = actividad(cancha, "2026-11-22", "10:00");
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        moverComoDjango(act, auditorio); detector.detectar(AHORA);
        moverComoDjango(act, cancha); detector.detectar(AHORA);

        assertEquals(AlertaUbicacion.CANCELADA, alertas.findByInscripcionId(i.getId()).get(0).getEstado());
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertTrue(enviados.isEmpty());
    }

    @Test void unCambioDespuesDeAvisarGeneraOtraAlerta() {
        ObjectId act = actividad(cancha, "2026-11-22", "10:00");
        inscribir("u1", act);
        celular("u1");
        moverComoDjango(act, auditorio); detector.detectar(AHORA); enviador.enviarPendientes(AHORA);
        moverComoDjango(act, laboratorio); detector.detectar(AHORA); enviador.enviarPendientes(AHORA);

        assertEquals(2, enviados.size());
        assertEquals("Auditorio principal", enviados.get(1).get("ubicacionAnterior"));
        assertEquals("Laboratorio 3", enviados.get(1).get("ubicacionNueva"));
    }

    @Test void actividadesCanceladasOSinUbicacionNoSeAvisan() {
        ObjectId cancelada = actividad(cancha, "2026-11-22", "10:00");
        ObjectId sinLugar = actividad(cancha, "2026-11-22", "10:00");
        inscribir("u1", cancelada); inscribir("u1", sinLugar);
        mongo.updateFirst(new Query(Criteria.where("_id").is(cancelada)),
                new Update().set("estado", "cancelada").set("ubicacionId", auditorio), "actividades");
        mongo.updateFirst(new Query(Criteria.where("_id").is(sinLugar)), new Update().unset("ubicacionId"), "actividades");

        assertEquals(0, detector.detectar(AHORA));
        assertEquals(0, alertas.count());
    }

    @Test void siLaActividadYaEmpezoSeActualizaSinAvisar() {
        ObjectId act = actividad(cancha, "2026-11-22", "07:00"); // ya empezó a las 08:00 de «ahora»
        Inscripcion i = inscribir("u1", act);
        moverComoDjango(act, auditorio);

        assertEquals(0, detector.detectar(AHORA));
        assertEquals(auditorio.toHexString(), inscripciones.findById(i.getId()).orElseThrow().getUbicacion());
        assertEquals(0, alertas.count());
    }

    @Test void unaUbicacionBorradaSeMuestraPorConfirmar() {
        ObjectId act = actividad(cancha, "2026-11-22", "10:00");
        inscribir("u1", act);
        celular("u1");
        mongo.remove(new Query(Criteria.where("_id").is(cancha)), "ubicaciones");
        moverComoDjango(act, auditorio);
        detector.detectar(AHORA);
        enviador.enviarPendientes(AHORA);
        assertEquals(NombresUbicacion.POR_CONFIRMAR, enviados.get(0).get("ubicacionAnterior"));
        assertEquals("Auditorio principal", enviados.get(0).get("ubicacionNueva"));
    }

    @Test void elRecordatorioMuestraElNombreDelLugarNuevo() {
        ObjectId act = actividad(cancha, "2026-11-22", "08:20"); // el recordatorio toca ya (30 min antes)
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        moverComoDjango(act, auditorio);
        detector.detectar(AHORA);

        var programador = new ProgramadorRecordatorios(inscripciones, recordatorios, 30, 48);
        var recordador = new EnviadorRecordatorios(recordatorios, inscripciones, dispositivos, mongo, push, new NombresUbicacion(mongo));
        programador.programar(AHORA);
        enviados.clear();
        assertEquals(1, recordador.enviarPendientes(AHORA));
        assertEquals("Torneo de fútbol empieza en 20 min (08:20) · Auditorio principal", enviados.get(0).get("cuerpo"));
        assertNotNull(i.getId());
    }
}
