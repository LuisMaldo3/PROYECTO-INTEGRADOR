package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.config.InteresesIniciales;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.AvisoPush;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Interes;
import pe.tecsup.vynk.repository.AvisoPushRepository;
import pe.tecsup.vynk.repository.DisponibilidadRepository;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;

/**
 * Elige qué actividad recomendarle a cada estudiante por notificación.
 *
 * Reglas, para que la notificación sea útil y no moleste:
 * - Solo estudiantes con la app en algún celular y con las recomendaciones activas (se apagan desde el Perfil).
 * - Como máximo una por día por estudiante, y solo entre las 09:00 y las 20:00 (hora de Perú).
 * - La actividad: programada, con cupos, empieza entre 2 horas y 7 días desde ahora, relacionada con sus intereses,
 *   sin inscribirse ni haberla descartado, sin cruce con su agenda ni con su horario de clases (si lo confirmó),
 *   y nunca recomendada antes por notificación.
 * Usa las mismas reglas de relevancia y de cruces que la pantalla de recomendaciones; no llama a la IA.
 */
@Service
public class SelectorRecomendaciones {
    private static final Logger log = LoggerFactory.getLogger(SelectorRecomendaciones.class);
    static final LocalTime DESDE = LocalTime.of(9, 0);
    static final LocalTime HASTA = LocalTime.of(20, 0);
    static final Duration ANTICIPACION_MINIMA = Duration.ofHours(2);
    static final Duration ANTICIPACION_MAXIMA = Duration.ofDays(7);
    private static final Duration CADA = Duration.ofMinutes(15);

    private final CatalogoController catalogo;
    private final DispositivoMovilRepository dispositivos;
    private final InscripcionRepository inscripciones;
    private final InteresesService intereses;
    private final DescartesRecomendacionesService descartes;
    private final DisponibilidadRepository disponibilidad;
    private final AvisoPushRepository avisos;
    private final PreferenciasNotificacionService preferencias;
    private final MongoTemplate mongo;
    private final boolean activo;
    private Instant ultimaRevision;

    public SelectorRecomendaciones(CatalogoController catalogo, DispositivoMovilRepository dispositivos,
                                   InscripcionRepository inscripciones, InteresesService intereses,
                                   DescartesRecomendacionesService descartes, DisponibilidadRepository disponibilidad,
                                   AvisoPushRepository avisos, PreferenciasNotificacionService preferencias,
                                   MongoTemplate mongo,
                                   @Value("${vynk.recomendaciones.notificar:true}") boolean activo) {
        this.catalogo = catalogo;
        this.dispositivos = dispositivos;
        this.inscripciones = inscripciones;
        this.intereses = intereses;
        this.descartes = descartes;
        this.disponibilidad = disponibilidad;
        this.avisos = avisos;
        this.preferencias = preferencias;
        this.mongo = mongo;
        this.activo = activo;
    }

    /** Se llama cada minuto, pero revisa como mucho cada 15 min. Devuelve cuántas recomendaciones registró. */
    public synchronized int elegir(Instant ahora) {
        if (!activo || !enHorario(ahora)) return 0;
        if (ultimaRevision != null && ahora.isBefore(ultimaRevision.plus(CADA))) return 0;
        ultimaRevision = ahora;
        Set<String> usuarios = new HashSet<>();
        for (DispositivoMovil d : dispositivos.findAll()) if (d.getUsuarioId() != null) usuarios.add(d.getUsuarioId());
        if (usuarios.isEmpty()) return 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> publicadas = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        if (publicadas == null || publicadas.isEmpty()) return 0;
        Instant inicioDelDia = ahora.atZone(ProgramadorRecordatorios.LIMA).toLocalDate()
                .atStartOfDay(ProgramadorRecordatorios.LIMA).toInstant();
        int registradas = 0;
        for (String usuario : usuarios) {
            try {
                if (!preferencias.recomendacionesActivas(usuario)) continue;
                if (avisos.countByUsuarioIdAndTipoAndCreadoEnGreaterThanEqual(usuario, AvisoPush.RECOMENDACION, inicioDelDia) > 0)
                    continue; // ya recibió la de hoy
                List<String> suyos = Optional.ofNullable(intereses.deUsuario(usuario).getIntereses()).orElse(List.of());
                if (suyos.isEmpty()) continue; // sin intereses no hay cómo elegir algo relevante
                List<Inscripcion> inscritas = inscripciones.findByEstudianteId(usuario).stream()
                        .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).toList();
                Set<String> notificadas = new HashSet<>();
                for (AvisoPush a : avisos.findByUsuarioIdAndTipo(usuario, AvisoPush.RECOMENDACION)) notificadas.add(a.getActividadId());
                List<BloqueOcupado> clases = disponibilidad.findByUsuarioId(usuario)
                        .filter(d -> d.isConfirmado() && d.getBloques() != null).map(d -> d.getBloques()).orElse(List.of());
                var elegida = elegirPara(publicadas, suyos, inscritas, descartes.idsDescartados(usuario), notificadas, clases, ahora);
                if (elegida.isEmpty()) continue;
                Map<String, Object> a = elegida.get();
                String actividadId = String.valueOf(a.get("id"));
                AvisoPush aviso = new AvisoPush();
                aviso.setClave("recomendacion:" + usuario + ":" + actividadId);
                aviso.setTipo(AvisoPush.RECOMENDACION);
                aviso.setUsuarioId(usuario);
                aviso.setActividadId(actividadId);
                aviso.setDatos(MensajeRecomendacion.datos(a, motivo(a, suyos)));
                // Pierde sentido una hora antes de empezar.
                aviso.setExpiraEn(inicio(a).minus(Duration.ofHours(1)));
                aviso.setCreadoEn(ahora);
                mongo.insert(aviso);
                registradas++;
            } catch (DuplicateKeyException e) {
                log.debug("La recomendación para {} ya existía", usuario);
            } catch (RuntimeException e) {
                log.error("No se pudo elegir la recomendación del usuario {}: {}", usuario, e.toString());
            }
        }
        return registradas;
    }

    static boolean enHorario(Instant ahora) {
        LocalTime hora = ahora.atZone(ProgramadorRecordatorios.LIMA).toLocalTime();
        return !hora.isBefore(DESDE) && hora.isBefore(HASTA);
    }

    /** La mejor actividad para este estudiante ahora, o vacío. Sin base de datos, para poder probarla. */
    static Optional<Map<String, Object>> elegirPara(List<Map<String, Object>> publicadas, List<String> intereses,
            List<Inscripcion> inscritas, Set<String> descartadas, Set<String> notificadas, List<BloqueOcupado> clases,
            Instant ahora) {
        Set<String> propias = new HashSet<>();
        for (Inscripcion i : inscritas) propias.add(i.getActividadId());
        List<Map<String, Object>> candidatas = new ArrayList<>();
        for (Map<String, Object> a : publicadas) {
            if (a == null || a.get("id") == null) continue;
            String id = String.valueOf(a.get("id"));
            if (propias.contains(id) || descartadas.contains(id) || notificadas.contains(id)) continue;
            if (!"programada".equalsIgnoreCase(String.valueOf(a.getOrDefault("estado", "programada")))) continue;
            if (!SugerenciasHuecosService.competenciaDisponible(a)) continue;
            Instant inicio = inicio(a);
            if (inicio == null || inicio.isBefore(ahora.plus(ANTICIPACION_MINIMA)) || inicio.isAfter(ahora.plus(ANTICIPACION_MAXIMA)))
                continue;
            if (AgendaSugeridaService.relevancia(a, intereses) <= 0) continue;
            candidatas.add(a);
        }
        List<Map<String, Object>> ordenadas = new ArrayList<>(SugerenciasHuecosService.filtrarCompatibles(candidatas, clases, inscritas));
        ordenadas.sort(Comparator.<Map<String, Object>>comparingInt(a -> -AgendaSugeridaService.relevancia(a, intereses))
                .thenComparing(a -> String.valueOf(a.get("fecha")))
                .thenComparing(a -> String.valueOf(a.get("horaInicio"))));
        List<Map<String, Object>> sinCruce = AgendaSugeridaService.sinCruces(inscritas, ordenadas, Map.of(), 1);
        return sinCruce.isEmpty() ? Optional.empty() : Optional.of(sinCruce.get(0));
    }

    static Instant inicio(Map<String, Object> a) {
        return Horario.de(a.get("fecha"), a.get("horaInicio"), a.get("horaFin")).comienzo();
    }

    /** «Porque te interesa Tecnología e innovación»: el primer interés que coincide, con su nombre legible. */
    static String motivo(Map<String, Object> a, List<String> intereses) {
        String texto = (a.get("nombre") + " " + a.get("descripcion") + " " + a.get("categoriaId")).toLowerCase(Locale.ROOT);
        Map<String, String> nombres = new LinkedHashMap<>();
        for (Interes i : InteresesIniciales.CATALOGO) nombres.put(i.getId(), i.getNombre());
        for (String i : intereses)
            if (i != null && !i.isBlank() && texto.contains(i.toLowerCase(Locale.ROOT)))
                return "Porque te interesa " + nombres.getOrDefault(i, i);
        return "";
    }
}
