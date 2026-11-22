package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.RecordatorioActividad;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.RecordatorioActividadRepository;

/** Envía al celular los recordatorios que ya les toca, verificando antes que la inscripción siga vigente. */
@Service
public class EnviadorRecordatorios {
    private static final Logger log = LoggerFactory.getLogger(EnviadorRecordatorios.class);
    static final int MAX_INTENTOS = 3;
    /** Un aviso «en envío» más tiempo que esto quedó huérfano (el servidor se cayó a la mitad). */
    private static final Duration ENVIO_ATASCADO = Duration.ofMinutes(5);

    private final RecordatorioActividadRepository recordatorios;
    private final InscripcionRepository inscripciones;
    private final DispositivoMovilRepository dispositivos;
    private final MongoTemplate mongo;
    private final NotificadorPush push;
    private final NombresUbicacion ubicaciones;

    public EnviadorRecordatorios(RecordatorioActividadRepository recordatorios, InscripcionRepository inscripciones,
                                 DispositivoMovilRepository dispositivos, MongoTemplate mongo, NotificadorPush push,
                                 NombresUbicacion ubicaciones) {
        this.recordatorios = recordatorios;
        this.inscripciones = inscripciones;
        this.dispositivos = dispositivos;
        this.mongo = mongo;
        this.push = push;
        this.ubicaciones = ubicaciones;
    }

    /** Envía los avisos vencidos. Devuelve cuántos recordatorios se entregaron. */
    public int enviarPendientes(Instant ahora) {
        if (!push.disponible()) return 0;
        liberarAtascados(ahora);
        int enviados = 0;
        Map<String, String> lugares = null; // se cargan solo si hay algo que enviar
        for (RecordatorioActividad r : recordatorios
                .findTop200ByEstadoAndEnviarEnLessThanEqualOrderByEnviarEnAsc(RecordatorioActividad.PENDIENTE, ahora)) {
            try {
                if (lugares == null) lugares = ubicaciones.cargar();
                if (procesar(r, ahora, lugares)) enviados++;
            } catch (RuntimeException e) {
                log.error("Falló el recordatorio {}: {}", r.getId(), e.toString());
            }
        }
        return enviados;
    }

    private boolean procesar(RecordatorioActividad pendiente, Instant ahora, Map<String, String> lugares) {
        if (!pendiente.getInicio().isAfter(ahora)) {
            // Ya empezó: un aviso tardío confundiría más de lo que ayuda.
            cerrar(pendiente.getId(), RecordatorioActividad.PENDIENTE, RecordatorioActividad.VENCIDO, null, null);
            return false;
        }
        RecordatorioActividad r = reclamar(pendiente.getId(), ahora);
        if (r == null) return false; // otra ejecución lo tomó
        try {
            Inscripcion inscripcion = inscripciones.findById(r.getInscripcionId()).orElse(null);
            if (inscripcion == null || !"inscrita".equalsIgnoreCase(inscripcion.getEstado())) {
                cerrar(r.getId(), RecordatorioActividad.ENVIANDO, RecordatorioActividad.CANCELADO, null, null);
                return false;
            }
            if (!r.getInicio().equals(ProgramadorRecordatorios.inicioDe(inscripcion))) {
                // La actividad cambió de hora: el programador corrige el aviso en su próximo ciclo.
                cerrar(r.getId(), RecordatorioActividad.ENVIANDO, RecordatorioActividad.PENDIENTE, null, null);
                return false;
            }
            List<DispositivoMovil> celulares = dispositivos.findByUsuarioId(r.getUsuarioId());
            if (celulares.isEmpty()) {
                // Todavía no abrió la app en ningún celular: se espera hasta que empiece la actividad.
                cerrar(r.getId(), RecordatorioActividad.ENVIANDO, RecordatorioActividad.PENDIENTE, null, null);
                return false;
            }
            Map<String, String> datos = MensajeRecordatorio.datos(inscripcion,
                    NombresUbicacion.nombre(lugares, inscripcion.getUbicacion()), r.getInicio(), ahora);
            int entregados = 0;
            boolean fallo = false;
            for (DispositivoMovil celular : celulares) {
                switch (push.enviar(celular.getToken(), datos)) {
                    case ENVIADO -> entregados++;
                    case TOKEN_INVALIDO -> dispositivos.deleteByToken(celular.getToken());
                    case ERROR_TEMPORAL, ERROR_PERMANENTE -> fallo = true;
                }
            }
            if (entregados > 0) {
                cerrar(r.getId(), RecordatorioActividad.ENVIANDO, RecordatorioActividad.ENVIADO, null, ahora);
                return true;
            }
            if (!fallo) { // todos sus celulares eran inválidos y se olvidaron
                cerrar(r.getId(), RecordatorioActividad.ENVIANDO, RecordatorioActividad.PENDIENTE, null, null);
                return false;
            }
            int intentos = r.getIntentos() + 1;
            cerrar(r.getId(), RecordatorioActividad.ENVIANDO,
                    intentos >= MAX_INTENTOS ? RecordatorioActividad.FALLIDO : RecordatorioActividad.PENDIENTE,
                    intentos, null);
            return false;
        } catch (RuntimeException e) {
            cerrar(r.getId(), RecordatorioActividad.ENVIANDO, RecordatorioActividad.PENDIENTE, r.getIntentos() + 1, null);
            throw e;
        }
    }

    /** Lo marca «enviando» solo si seguía pendiente: así dos ejecuciones nunca mandan el mismo aviso. */
    private RecordatorioActividad reclamar(String id, Instant ahora) {
        return mongo.findAndModify(
                new Query(Criteria.where("_id").is(id).and("estado").is(RecordatorioActividad.PENDIENTE)),
                new Update().set("estado", RecordatorioActividad.ENVIANDO).set("reclamadoEn", ahora),
                FindAndModifyOptions.options().returnNew(true), RecordatorioActividad.class);
    }

    private void cerrar(String id, String estadoActual, String nuevoEstado, Integer intentos, Instant enviadoEn) {
        Update cambio = new Update().set("estado", nuevoEstado).unset("reclamadoEn");
        if (intentos != null) cambio.set("intentos", intentos);
        if (enviadoEn != null) cambio.set("enviadoEn", enviadoEn);
        mongo.updateFirst(new Query(Criteria.where("_id").is(id).and("estado").is(estadoActual)),
                cambio, RecordatorioActividad.class);
    }

    private void liberarAtascados(Instant ahora) {
        mongo.updateMulti(new Query(Criteria.where("estado").is(RecordatorioActividad.ENVIANDO)
                        .and("reclamadoEn").lt(ahora.minus(ENVIO_ATASCADO))),
                new Update().set("estado", RecordatorioActividad.PENDIENTE).unset("reclamadoEn"),
                RecordatorioActividad.class);
    }
}
