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
import pe.tecsup.vynk.model.AlertaHorario;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.repository.AlertaHorarioRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.RecordatorioActividadRepository;

/**
 * Cambio de horario de punta a punta contra un MongoDB real, editando la actividad como lo hace Django
 * (solo la colección «actividades»). Se activa con MONGODB_E2E_URI, igual que RecordatoriosMongoE2ETest.
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class CambiosHorarioMongoE2ETest {
    static final Instant AHORA = Instant.parse("2026-11-22T13:00:00Z"); // 08:00 en Lima

    MongoClient cliente;
    MongoTemplate mongo;
    InscripcionRepository inscripciones;
    AlertaHorarioRepository alertas;
    RecordatorioActividadRepository recordatorios;
    DispositivoMovilRepository dispositivos;
    final List<Map<String, String>> enviados = Collections.synchronizedList(new ArrayList<>());
    DetectorCambiosHorario detector;
    EnviadorAlertasHorario enviador;

    final NotificadorPush push = new NotificadorPush() {
        @Override public boolean disponible() { return true; }
        @Override public Resultado enviar(String token, Map<String, String> datos, Duration vida) {
            enviados.add(datos);
            return Resultado.ENVIADO;
        }
    };

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var fabrica = new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_horario_" + System.nanoTime());
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(fabrica, convertidor);
        var resolvedor = new MongoPersistentEntityIndexResolver(contexto);
        for (Class<?> tipo : List.of(AlertaHorario.class, RecordatorioActividad.class, DispositivoMovil.class, Inscripcion.class))
            for (var indice : resolvedor.resolveIndexFor(tipo)) mongo.indexOps(tipo).createIndex(indice);
        var repos = new MongoRepositoryFactory(mongo);
        inscripciones = repos.getRepository(InscripcionRepository.class);
        alertas = repos.getRepository(AlertaHorarioRepository.class);
        recordatorios = repos.getRepository(RecordatorioActividadRepository.class);
        dispositivos = repos.getRepository(DispositivoMovilRepository.class);
        detector = new DetectorCambiosHorario(mongo);
        enviador = new EnviadorAlertasHorario(alertas, inscripciones, dispositivos, mongo, push);
    }

    @AfterEach void limpiar() {
        mongo.getDb().drop();
        cliente.close();
    }

    ObjectId actividad(String fecha, String inicio, String fin) {
        ObjectId id = new ObjectId();
        Document doc = new Document("_id", id).append("nombre", "Torneo de fútbol").append("estado", "programada")
                .append("fecha", fecha).append("horaInicio", inicio).append("horaFin", fin);
        mongo.insert(doc, "actividades");
        return id;
    }

    /** Así edita el horario el panel de administración (Django): solo cambia la actividad. */
    void editarComoDjango(ObjectId id, String fecha, String inicio, String fin) {
        mongo.updateFirst(new Query(Criteria.where("_id").is(id)),
                new Update().set("fecha", fecha).set("horaInicio", inicio).set("horaFin", fin), "actividades");
    }

    /** Copia el horario igual que InscripcionesCompetenciaController al inscribir (null pasa a ""). */
    Inscripcion inscribir(String estudiante, ObjectId actividad) {
        Document a = mongo.findById(actividad, Document.class, "actividades");
        Inscripcion i = new Inscripcion();
        i.setEstudianteId(estudiante); i.setActividadId(actividad.toHexString()); i.setNombreActividad("Torneo de fútbol");
        i.setFecha(String.valueOf(a.get("fecha"))); i.setHoraInicio(String.valueOf(a.get("horaInicio")));
        i.setHoraFin(a.get("horaFin") == null ? "" : String.valueOf(a.get("horaFin")));
        i.setEstado("inscrita");
        return inscripciones.save(i);
    }

    void celular(String usuario) {
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId(usuario); d.setToken("token-" + usuario + "-0123456789abcdef");
        dispositivos.save(d);
    }

    @Test void detectaElCambioAvisaAlInscritoConElHorarioAnteriorYElNuevoYActualizaSuAgenda() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        assertEquals(0, detector.detectar(AHORA), "sin cambios no hay alertas");

        editarComoDjango(act, "2026-11-23", "15:00", "16:30");

        assertEquals(1, detector.detectar(AHORA));
        Inscripcion actualizada = inscripciones.findById(i.getId()).orElseThrow();
        assertEquals("2026-11-23", actualizada.getFecha());
        assertEquals("15:00", actualizada.getHoraInicio());
        assertEquals("16:30", actualizada.getHoraFin());
        AlertaHorario a = alertas.findByInscripcionId(i.getId()).get(0);
        assertEquals("10:00", a.getInicioAnterior());
        assertEquals("15:00", a.getInicioNuevo());
        assertEquals(Instant.parse("2026-11-23T20:00:00Z"), a.getComienzaEn());

        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals("dom 22/11 · 10:00–11:00", enviados.get(0).get("horarioAnterior"));
        assertEquals("lun 23/11 · 15:00–16:30", enviados.get(0).get("horarioNuevo"));
        assertEquals(act.toHexString(), enviados.get(0).get("actividadId"));

        // El mismo cambio no se vuelve a detectar ni a enviar.
        assertEquals(0, detector.detectar(AHORA.plusSeconds(60)));
        assertEquals(0, enviador.enviarPendientes(AHORA.plusSeconds(60)));
        assertEquals(1, enviados.size());
    }

    @Test void avisaATodosLosInscritosPeroNoALosQueCancelaron() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        inscribir("u1", act); inscribir("u2", act);
        Inscripcion cancelada = inscribir("u3", act);
        cancelada.setEstado("cancelada"); inscripciones.save(cancelada);
        celular("u1"); celular("u2"); celular("u3");

        editarComoDjango(act, "2026-11-22", "12:00", "13:00");

        assertEquals(2, detector.detectar(AHORA));
        assertEquals(2, enviador.enviarPendientes(AHORA));
        assertTrue(alertas.findByInscripcionId(cancelada.getId()).isEmpty());
    }

    @Test void soloCambiarLaHoraDeFinTambienSeAvisa() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        inscribir("u1", act);
        editarComoDjango(act, "2026-11-22", "10:00", "12:00");
        assertEquals(1, detector.detectar(AHORA));
    }

    @Test void nullYVacioEnLaHoraDeFinNoCuentanComoCambio() {
        ObjectId act = actividad("2026-11-22", "10:00", null);
        inscribir("u1", act);
        assertEquals(0, detector.detectar(AHORA));
        editarComoDjango(act, "2026-11-22", "10:00", "");
        assertEquals(0, detector.detectar(AHORA));
    }

    @Test void dosCambiosAntesDeEnviarSeJuntanEnUnaSolaAlerta() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        editarComoDjango(act, "2026-11-22", "12:00", "13:00");
        detector.detectar(AHORA);
        editarComoDjango(act, "2026-11-22", "14:00", "15:00");
        detector.detectar(AHORA);

        List<AlertaHorario> todas = alertas.findByInscripcionId(i.getId());
        assertEquals(1, todas.size());
        assertEquals("10:00", todas.get(0).getInicioAnterior(), "conserva el horario original");
        assertEquals("14:00", todas.get(0).getInicioNuevo(), "y muestra el más reciente");
    }

    @Test void siElHorarioVuelveAlDeAntesAntesDeEnviarNoSeAvisa() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        editarComoDjango(act, "2026-11-22", "12:00", "13:00");
        detector.detectar(AHORA);
        editarComoDjango(act, "2026-11-22", "10:00", "11:00");
        detector.detectar(AHORA);

        assertEquals(AlertaHorario.CANCELADA, alertas.findByInscripcionId(i.getId()).get(0).getEstado());
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertTrue(enviados.isEmpty());
    }

    @Test void unCambioDespuesDeAvisarGeneraOtraAlerta() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        celular("u1");
        editarComoDjango(act, "2026-11-22", "12:00", "13:00");
        detector.detectar(AHORA); enviador.enviarPendientes(AHORA);
        editarComoDjango(act, "2026-11-22", "14:00", "15:00");
        detector.detectar(AHORA); enviador.enviarPendientes(AHORA);

        assertEquals(2, enviados.size());
        assertEquals("dom 22/11 · 12:00–13:00", enviados.get(1).get("horarioAnterior"));
        assertEquals(2, alertas.findByInscripcionId(i.getId()).size());
    }

    @Test void actividadesCanceladasFinalizadasOConHorarioIncompletoNoSeAvisan() {
        ObjectId cancelada = actividad("2026-11-22", "10:00", "11:00");
        ObjectId incompleta = actividad("2026-11-22", "10:00", "11:00");
        inscribir("u1", cancelada); inscribir("u1", incompleta);
        mongo.updateFirst(new Query(Criteria.where("_id").is(cancelada)),
                new Update().set("estado", "cancelada").set("horaInicio", "12:00"), "actividades");
        editarComoDjango(incompleta, "", "", "");

        assertEquals(0, detector.detectar(AHORA));
        assertEquals(0, alertas.count());
    }

    @Test void siElNuevoHorarioYaPasoSeActualizaLaAgendaSinAvisar() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        editarComoDjango(act, "2026-11-22", "07:00", "07:30"); // antes de las 08:00 de «ahora»

        assertEquals(0, detector.detectar(AHORA));
        assertEquals("07:00", inscripciones.findById(i.getId()).orElseThrow().getHoraInicio());
        assertEquals(0, alertas.count());
    }

    @Test void elRecordatorioSeReprogramaConLaHoraNueva() {
        ObjectId act = actividad("2026-11-22", "10:00", "11:00");
        Inscripcion i = inscribir("u1", act);
        var programador = new ProgramadorRecordatorios(inscripciones, recordatorios, 30, 48);
        programador.programar(AHORA);
        assertEquals(Instant.parse("2026-11-22T15:00:00Z"), recordatorios.findByInscripcionId(i.getId()).orElseThrow().getInicio());

        editarComoDjango(act, "2026-11-22", "12:00", "13:00");
        detector.detectar(AHORA);
        programador.programar(AHORA);

        assertEquals(Instant.parse("2026-11-22T17:00:00Z"), recordatorios.findByInscripcionId(i.getId()).orElseThrow().getInicio());
    }

    @Test void actividadesConIdDeTextoTambienSeDetectan() {
        mongo.insert(new Document("_id", "act-texto").append("nombre", "Charla").append("estado", "programada")
                .append("fecha", "2026-11-22").append("horaInicio", "10:00").append("horaFin", ""), "actividades");
        Inscripcion i = new Inscripcion();
        i.setEstudianteId("u1"); i.setActividadId("act-texto"); i.setFecha("2026-11-22");
        i.setHoraInicio("10:00"); i.setHoraFin(""); i.setEstado("inscrita");
        inscripciones.save(i);
        mongo.updateFirst(new Query(Criteria.where("_id").is("act-texto")), new Update().set("horaInicio", "11:00"), "actividades");

        assertEquals(1, detector.detectar(AHORA));
    }
}
