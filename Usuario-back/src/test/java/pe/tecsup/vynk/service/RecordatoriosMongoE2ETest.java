package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;

/**
 * Prueba de punta a punta de los recordatorios contra un MongoDB de verdad (con el envío a Firebase simulado).
 * No corre sola, porque necesita una base de datos: se activa con la variable de entorno
 * MONGODB_E2E_URI, por ejemplo  MONGODB_E2E_URI=mongodb://localhost:27017  mvn test -Dtest=RecordatoriosMongoE2ETest
 * Usa una base temporal que borra al terminar; no toca la base «vynk».
 */
@EnabledIfEnvironmentVariable(named = "MONGODB_E2E_URI", matches = ".+")
class RecordatoriosMongoE2ETest {
    // 22/11/2026 09:35 en Lima; la actividad empieza a las 10:00 (15:00 UTC), así que el aviso ya toca (30 min antes).
    static final Instant AHORA = Instant.parse("2026-11-22T14:35:00Z");

    MongoClient cliente;
    MongoTemplate mongo;
    pe.tecsup.vynk.repository.InscripcionRepository inscripciones;
    pe.tecsup.vynk.repository.RecordatorioActividadRepository recordatorios;
    pe.tecsup.vynk.repository.DispositivoMovilRepository dispositivos;
    Falso push;
    ProgramadorRecordatorios programador;
    EnviadorRecordatorios enviador;

    /** Notificador que anota a quién «envió» el aviso. */
    static class Falso implements NotificadorPush {
        final List<String> tokens = Collections.synchronizedList(new ArrayList<>());
        final List<Map<String, String>> mensajes = Collections.synchronizedList(new ArrayList<>());
        volatile Resultado resultado = Resultado.ENVIADO;
        @Override public boolean disponible() { return true; }
        @Override public Resultado enviar(String token, Map<String, String> datos, java.time.Duration vida) {
            tokens.add(token); mensajes.add(datos);
            return resultado;
        }
    }

    @BeforeEach void preparar() {
        cliente = MongoClients.create(System.getenv("MONGODB_E2E_URI"));
        var fabrica = new SimpleMongoClientDatabaseFactory(cliente, "vynk_e2e_" + System.nanoTime());
        // Igual que la configuración de Spring Boot: con las conversiones estándar (fechas Instant, etc.).
        var conversiones = new MongoCustomConversions(List.of());
        var contexto = new MongoMappingContext();
        contexto.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
        contexto.setAutoIndexCreation(false);
        var convertidor = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, contexto);
        convertidor.setCustomConversions(conversiones);
        convertidor.afterPropertiesSet();
        mongo = new MongoTemplate(fabrica, convertidor);
        // En el servidor los índices los crea spring.data.mongodb.auto-index-creation=true; aquí se crean igual.
        var resolvedor = new MongoPersistentEntityIndexResolver(contexto);
        for (Class<?> tipo : List.of(RecordatorioActividad.class, DispositivoMovil.class, Inscripcion.class)) {
            for (var indice : resolvedor.resolveIndexFor(tipo)) mongo.indexOps(tipo).createIndex(indice);
        }
        var repositorios = new MongoRepositoryFactory(mongo);
        inscripciones = repositorios.getRepository(pe.tecsup.vynk.repository.InscripcionRepository.class);
        recordatorios = repositorios.getRepository(pe.tecsup.vynk.repository.RecordatorioActividadRepository.class);
        dispositivos = repositorios.getRepository(pe.tecsup.vynk.repository.DispositivoMovilRepository.class);
        push = new Falso();
        programador = new ProgramadorRecordatorios(inscripciones, recordatorios, 30, 48);
        enviador = new EnviadorRecordatorios(recordatorios, inscripciones, dispositivos, mongo, push);
    }

    @AfterEach void limpiar() {
        mongo.getDb().drop();
        cliente.close();
    }

    Inscripcion inscribir(String estudiante, String actividad, String hora) {
        Inscripcion i = new Inscripcion();
        i.setEstudianteId(estudiante); i.setActividadId(actividad); i.setNombreActividad("Torneo de fútbol");
        i.setUbicacion("Cancha 2"); i.setFecha("2026-11-22"); i.setHoraInicio(hora); i.setEstado("inscrita");
        return inscripciones.save(i);
    }

    void registrarCelular(String usuario, String token) {
        DispositivoMovil d = new DispositivoMovil();
        d.setUsuarioId(usuario); d.setToken(token); d.setCreadoEn(AHORA); d.setActualizadoEn(AHORA);
        dispositivos.save(d);
    }

    RecordatorioActividad recordatorioDe(Inscripcion i) {
        return recordatorios.findByInscripcionId(i.getId()).orElseThrow();
    }

    @Test void programaYEnviaUnaSolaVezAunqueSeReinicieElServidor() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        registrarCelular("u1", "token-celular-u1-0123456789");

        assertEquals(1, programador.programar(AHORA));
        RecordatorioActividad r = recordatorioDe(i);
        assertEquals(RecordatorioActividad.PENDIENTE, r.getEstado());
        assertEquals(Instant.parse("2026-11-22T15:00:00Z"), r.getInicio());
        assertEquals(Instant.parse("2026-11-22T14:30:00Z"), r.getEnviarEn());

        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals(List.of("token-celular-u1-0123456789"), push.tokens);
        assertEquals("act1", push.mensajes.get(0).get("actividadId"));
        r = recordatorioDe(i);
        assertEquals(RecordatorioActividad.ENVIADO, r.getEstado());
        assertNotNull(r.getEnviadoEn());
        assertNull(r.getReclamadoEn());

        // «Reinicio»: servicios nuevos sobre la misma base no repiten el aviso.
        var programador2 = new ProgramadorRecordatorios(inscripciones, recordatorios, 30, 48);
        var enviador2 = new EnviadorRecordatorios(recordatorios, inscripciones, dispositivos, mongo, push);
        assertEquals(0, programador2.programar(AHORA.plusSeconds(60)));
        assertEquals(0, enviador2.enviarPendientes(AHORA.plusSeconds(60)));
        assertEquals(1, push.tokens.size());
        assertEquals(1, recordatorios.count());
    }

    @Test void noEnviaAntesDeLaHoraDelAvisoYLoEnviaCuandoLlega() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        registrarCelular("u1", "token-celular-u1-0123456789");
        Instant temprano = Instant.parse("2026-11-22T13:00:00Z"); // 08:00 en Lima: faltan 2 h

        assertEquals(1, programador.programar(temprano));
        assertEquals(0, enviador.enviarPendientes(temprano));
        assertTrue(push.tokens.isEmpty());

        assertEquals(1, enviador.enviarPendientes(Instant.parse("2026-11-22T14:30:00Z")));
        assertEquals(RecordatorioActividad.ENVIADO, recordatorioDe(i).getEstado());
    }

    @Test void siElEstudianteCancelaAntesDelAvisoNoSeEnvia() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        registrarCelular("u1", "token-celular-u1-0123456789");
        programador.programar(AHORA);

        i.setEstado("cancelada");
        inscripciones.save(i);

        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertTrue(push.tokens.isEmpty());
        assertEquals(RecordatorioActividad.CANCELADO, recordatorioDe(i).getEstado());
    }

    @Test void siElOrganizadorCambiaLaHoraElAvisoSaleConLaHoraNueva() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        registrarCelular("u1", "token-celular-u1-0123456789");
        programador.programar(AHORA);

        i.setHoraInicio("10:20");
        inscripciones.save(i);

        assertEquals(1, programador.programar(AHORA));
        assertEquals(Instant.parse("2026-11-22T15:20:00Z"), recordatorioDe(i).getInicio());
        assertEquals(Instant.parse("2026-11-22T14:50:00Z"), recordatorioDe(i).getEnviarEn());
        // Con la hora nueva el aviso ya no toca todavía (faltan 45 min): sale 30 min antes de las 10:20.
        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(1, enviador.enviarPendientes(Instant.parse("2026-11-22T14:50:00Z")));
        assertTrue(push.mensajes.get(0).get("cuerpo").contains("empieza en 30 min (10:20)"), push.mensajes.get(0).get("cuerpo"));
    }

    @Test void sinCelularRegistradoEsperaYAvisaCuandoElEstudianteAbreLaApp() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        programador.programar(AHORA);

        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertEquals(RecordatorioActividad.PENDIENTE, recordatorioDe(i).getEstado());
        assertEquals(0, recordatorioDe(i).getIntentos());

        registrarCelular("u1", "token-celular-u1-0123456789");
        assertEquals(1, enviador.enviarPendientes(AHORA.plusSeconds(120)));
    }

    @Test void siLaActividadYaEmpezoSinAvisoElRecordatorioVence() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        programador.programar(AHORA);

        assertEquals(0, enviador.enviarPendientes(Instant.parse("2026-11-22T15:01:00Z")));
        assertEquals(RecordatorioActividad.VENCIDO, recordatorioDe(i).getEstado());
        assertTrue(push.tokens.isEmpty());
    }

    @Test void cadaEstudianteRecibeSoloSuPropioAviso() {
        inscribir("u1", "act1", "10:00");
        inscribir("u2", "act1", "10:00");
        registrarCelular("u1", "token-celular-u1-0123456789");
        registrarCelular("u2", "token-celular-u2-0123456789");

        assertEquals(2, programador.programar(AHORA));
        assertEquals(2, enviador.enviarPendientes(AHORA));
        assertEquals(2, push.tokens.size());
        assertTrue(push.tokens.containsAll(List.of("token-celular-u1-0123456789", "token-celular-u2-0123456789")));
    }

    @Test void unCelularInvalidoSeOlvidaYElAvisoNoSePierde() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        registrarCelular("u1", "token-viejo-0123456789abcdef");
        programador.programar(AHORA);
        push.resultado = NotificadorPush.Resultado.TOKEN_INVALIDO;

        assertEquals(0, enviador.enviarPendientes(AHORA));
        assertTrue(dispositivos.findByUsuarioId("u1").isEmpty());
        assertEquals(RecordatorioActividad.PENDIENTE, recordatorioDe(i).getEstado());

        registrarCelular("u1", "token-nuevo-0123456789abcdef");
        push.resultado = NotificadorPush.Resultado.ENVIADO;
        assertEquals(1, enviador.enviarPendientes(AHORA.plusSeconds(60)));
    }

    @Test void hilosSimultaneosNuncaEnvianElMismoAvisoDosVeces() throws Exception {
        for (int n = 0; n < 5; n++) {
            inscribir("u" + n, "act1", "10:00");
            registrarCelular("u" + n, "token-celular-u" + n + "-0123456789");
        }
        programador.programar(AHORA);

        int hilos = 8;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<java.util.concurrent.Future<Integer>> trabajos = new ArrayList<>();
        for (int h = 0; h < hilos; h++) {
            var propio = new EnviadorRecordatorios(recordatorios, inscripciones, dispositivos, mongo, push);
            trabajos.add(pool.submit(() -> { salida.await(); return propio.enviarPendientes(AHORA); }));
        }
        salida.countDown();
        int total = 0;
        for (var t : trabajos) total += t.get();
        pool.shutdown();

        assertEquals(5, total, "cada aviso se entrega una sola vez en total");
        assertEquals(5, push.tokens.size());
        assertEquals(5, new java.util.HashSet<>(push.tokens).size(), "ningún celular recibe el aviso repetido");
    }

    @Test void unEnvioQueQuedoATresMinutosDeCaerseSeRecuperaPasadosCincoMinutos() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        registrarCelular("u1", "token-celular-u1-0123456789");
        programador.programar(AHORA);
        RecordatorioActividad r = recordatorioDe(i);
        r.setEstado(RecordatorioActividad.ENVIANDO);
        r.setReclamadoEn(AHORA.minusSeconds(600));
        recordatorios.save(r);

        assertEquals(1, enviador.enviarPendientes(AHORA));
        assertEquals(RecordatorioActividad.ENVIADO, recordatorioDe(i).getEstado());
    }

    @Test void elIndiceUnicoImpideDosRecordatoriosParaLaMismaInscripcion() {
        Inscripcion i = inscribir("u1", "act1", "10:00");
        programador.programar(AHORA);

        RecordatorioActividad duplicado = new RecordatorioActividad();
        duplicado.setInscripcionId(i.getId()); duplicado.setUsuarioId("u1"); duplicado.setInicio(AHORA);
        assertThrows(DuplicateKeyException.class, () -> recordatorios.save(duplicado));
    }

    @Test void unMismoTokenSoloPuedeSerDeUnaCuenta() {
        registrarCelular("u1", "token-celular-u1-0123456789");
        DispositivoMovil otro = new DispositivoMovil();
        otro.setUsuarioId("u2"); otro.setToken("token-celular-u1-0123456789");
        assertThrows(DuplicateKeyException.class, () -> dispositivos.save(otro));
    }
}
