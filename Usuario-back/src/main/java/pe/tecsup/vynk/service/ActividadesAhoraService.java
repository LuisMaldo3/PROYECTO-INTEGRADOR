package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/**
 * US-78 T1: actividades disponibles en este momento. Responde al botón «¿Qué hay ahora?» del chat (y a preguntas
 * como «¿qué puedo hacer ahorita?») y a {@code GET /api/app/ahora}.
 *
 * <ul>
 *   <li><b>En curso</b>: ya empezaron y terminan dentro de al menos {@value #MINIMO_PARA_LLEGAR} minutos
 *       (si quedan menos, ya no vale la pena ir).</li>
 *   <li><b>Pronto</b>: empiezan dentro de la próxima hora.</li>
 * </ul>
 * Solo programadas, con cupos y de hoy (hora de Lima). Cada una dice si el estudiante ya está inscrito o si se cruza
 * con alguna de sus inscripciones. Todo sale del catálogo publicado: no se inventan horarios.
 */
@Service
public class ActividadesAhoraService {
    static final int MINIMO_PARA_LLEGAR = 10;
    static final int VENTANA_PRONTO = 60;
    static final int MAXIMO = 7;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<String> RELLENO = Set.of("que", "hay", "ahora", "ahorita", "mismo", "en", "este", "momento",
            "actividad", "actividades", "puedo", "podria", "hacer", "ir", "a", "de", "disponible", "disponibles", "algo",
            "para", "mi", "me", "el", "la", "los", "las", "y", "se", "esta", "estan", "pasando", "ocurriendo", "sucede",
            "cual", "cuales", "busca", "buscame", "dime", "muestrame", "hoy", "por", "favor", "empezando", "empieza", "pronto",
            "eventos", "evento", "tecsup", "campus", "aqui");

    private final CatalogoController catalogo;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public ActividadesAhoraService(CatalogoController catalogo, InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.catalogo = catalogo;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    /** «¿Qué hay ahora?», «¿qué puedo hacer ahorita?», «actividades en este momento»… sin tema, fecha ni hora. */
    public static boolean esPreguntaAhora(String pregunta) {
        String q = plano(pregunta);
        if (!q.matches(".*\\b(ahora|ahorita|en este momento)\\b.*")) return false;
        for (String palabra : q.split(" ")) if (!palabra.isBlank() && !RELLENO.contains(palabra)) return false;
        return true;
    }

    /** Respuesta del asistente (se guarda en la conversación como cualquier otra). */
    public AsistenteHorariosService.Respuesta responder(String sujeto, Instant ahora) {
        Map<String, Object> r = buscar(sujeto, ahora);
        @SuppressWarnings("unchecked") var actividades = (List<Map<String, Object>>) r.get("actividades");
        return new AsistenteHorariosService.Respuesta(String.valueOf(r.get("mensaje")), "catalogo",
                "Actividades de hoy consultadas a las " + r.get("hora") + " (hora de Lima) en el catálogo publicado.",
                actividades, List.of(), "ahora", List.of());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> buscar(String sujeto, Instant ahora) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(sujeto).or(() -> usuarios.findByCorreo(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        List<Map<String, Object>> publicadas = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        return armar(publicadas == null ? List.of() : publicadas, inscripciones.findByEstudianteId(usuario.getId()),
                LocalDateTime.ofInstant(ahora, LIMA));
    }

    /** Sin acceso a datos: se prueba directamente. */
    static Map<String, Object> armar(List<Map<String, Object>> catalogo, List<Inscripcion> mias, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<Inscripcion> activas = mias == null ? List.of() : mias.stream()
                .filter(i -> i != null && "inscrita".equalsIgnoreCase(i.getEstado())).toList();
        List<Map<String, Object>> enCurso = new ArrayList<>(), pronto = new ArrayList<>();
        Map<String, Object> siguiente = null;
        LocalDateTime inicioSiguiente = null;
        for (Map<String, Object> a : catalogo) {
            if (a == null || !"programada".equalsIgnoreCase(texto(a.get("estado"), "programada"))) continue;
            if (a.get("cuposDisponibles") instanceof Number n && n.intValue() <= 0) continue;
            var h = AgendaSugeridaService.horario(texto(a.get("fecha"), ""), texto(a.get("horaInicio"), ""), texto(a.get("horaFin"), ""));
            if (h == null || !h.fecha().equals(hoy)) continue;
            LocalDateTime ini = LocalDateTime.of(h.fecha(), h.inicio()), fin = LocalDateTime.of(h.fecha(), h.fin());
            long paraTerminar = Duration.between(ahora, fin).toMinutes();
            long paraEmpezar = Duration.between(ahora, ini).toMinutes();
            boolean curso = !ahora.isBefore(ini) && ahora.isBefore(fin) && paraTerminar >= MINIMO_PARA_LLEGAR;
            boolean empiezaPronto = ini.isAfter(ahora) && paraEmpezar <= VENTANA_PRONTO;
            if (!curso && !empiezaPronto) {
                if (ini.isAfter(ahora) && (inicioSiguiente == null || ini.isBefore(inicioSiguiente))) {
                    inicioSiguiente = ini;
                    siguiente = a;
                }
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(a);
            String id = texto(a.get("id"), "");
            boolean inscrito = activas.stream().anyMatch(i -> id.equals(i.getActividadId()));
            String cruza = inscrito ? null : activas.stream()
                    .filter(i -> { var b = AgendaSugeridaService.horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin());
                                   return b != null && b.cruza(h); })
                    .map(i -> texto(i.getNombreActividad(), "otra actividad")).findFirst().orElse(null);
            item.put("estadoAhora", curso ? "en_curso" : "pronto");
            item.put("minutos", curso ? paraTerminar : paraEmpezar);
            item.put("detalleAhora", curso
                    ? "En curso · termina en " + duracion(paraTerminar) + " (" + h.fin() + ")"
                    : "Empieza en " + duracion(paraEmpezar) + " (" + h.inicio() + ")");
            item.put("inscrito", inscrito);
            item.put("cruzaCon", cruza);
            boolean equipos = RecomendacionesAppService.porEquipos(a);
            item.put("inscripcionDirecta", !inscrito && !equipos);
            (curso ? enCurso : pronto).add(item);
        }
        // En curso: primero las que más tiempo tienen por delante. Pronto: primero las que empiezan antes.
        enCurso.sort(Comparator.comparingLong((Map<String, Object> m) -> -((Number) m.get("minutos")).longValue()));
        pronto.sort(Comparator.comparingLong((Map<String, Object> m) -> ((Number) m.get("minutos")).longValue()));
        List<Map<String, Object>> todas = new ArrayList<>(enCurso);
        todas.addAll(pronto);
        List<Map<String, Object>> elegidas = todas.size() > MAXIMO ? todas.subList(0, MAXIMO) : todas;

        String hora = String.format("%02d:%02d", ahora.getHour(), ahora.getMinute());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("hora", hora);
        r.put("enCurso", enCurso.size());
        r.put("pronto", pronto.size());
        r.put("mensaje", mensaje(hora, enCurso.size(), pronto.size(), elegidas, siguiente));
        r.put("actividades", new ArrayList<>(elegidas));
        r.put("siguiente", siguiente == null ? null : Map.of("id", texto(siguiente.get("id"), ""),
                "nombre", texto(siguiente.get("nombre"), "Actividad"), "horaInicio", texto(siguiente.get("horaInicio"), "")));
        return r;
    }

    static String mensaje(String hora, int enCurso, int pronto, List<Map<String, Object>> elegidas, Map<String, Object> siguiente) {
        if (enCurso == 0 && pronto == 0) {
            String base = "Ahora mismo (" + hora + ") no hay actividades en curso ni empieza ninguna en la próxima hora.";
            return siguiente == null ? base + " Por hoy ya no quedan actividades con cupos."
                    : base + " La siguiente de hoy es «" + texto(siguiente.get("nombre"), "una actividad") + "» a las "
                      + texto(siguiente.get("horaInicio"), "") + ".";
        }
        StringBuilder s = new StringBuilder("Ahora mismo (" + hora + ") ");
        if (enCurso > 0) s.append(enCurso == 1 ? "hay 1 actividad en curso" : "hay " + enCurso + " actividades en curso");
        if (enCurso > 0 && pronto > 0) s.append(" y ");
        if (pronto > 0) s.append(pronto == 1 ? "1 empieza dentro de la próxima hora" : pronto + " empiezan dentro de la próxima hora");
        s.append(":");
        // El detalle va en viñetas: la app lo muestra en tarjetas; la web y el historial lo leen como texto.
        s.append("\n");
        for (var a : elegidas) {
            s.append("\n• ").append(texto(a.get("nombre"), "Actividad")).append(" — ").append(a.get("detalleAhora"));
            String lugar = texto(a.get("ubicacion"), "");
            if (!lugar.isBlank()) s.append(" · ").append(lugar);
        }
        return s.toString();
    }

    static String duracion(long minutos) {
        if (minutos < 60) return minutos + " min";
        long h = minutos / 60, m = minutos % 60;
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    static String plano(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
    }

    private static String texto(Object v, String porDefecto) {
        String s = v == null ? "" : String.valueOf(v).strip();
        return s.isEmpty() || "null".equals(s) ? porDefecto : s;
    }
}
