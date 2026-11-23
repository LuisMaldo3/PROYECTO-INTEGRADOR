package pe.tecsup.vynk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/**
 * US-79 T1: alternativas para una actividad a la que el estudiante no puede asistir
 * ({@code GET /api/app/alternativas/{actividadId}?motivo=horario|otro}).
 *
 * <ul>
 *   <li>Solo para una actividad en la que está inscrito.</li>
 *   <li>Candidatas: programadas, con cupos, futuras, en las que no está inscrito y que no se cruzan con sus otras
 *       inscripciones (el horario de la actividad que deja queda libre).</li>
 *   <li>Con motivo «horario» (no puede a esa hora) se descartan las que coinciden con ese horario.</li>
 *   <li>Si confirmó su horario de clases, tampoco se proponen actividades en horas de clase.</li>
 *   <li>Se ordenan por parecido (área, categoría, modalidad, palabras del nombre) y cercanía. Si hay clave de Gemini,
 *       la IA elige el orden y explica cada una, pero solo entre esas candidatas; si no responde, se usan las reglas.</li>
 *   <li>{@code puedeCambiar}: el capitán de un equipo no puede cambiarse en un toque (cancelar retiraría al equipo).</li>
 * </ul>
 */
@Service
public class AlternativasAsistenciaService {
    static final int MAXIMO = 5;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d", Locale.forLanguageTag("es-PE"));

    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final CatalogoController catalogo;
    private final ObjectMapper json;
    private final DisponibilidadService disponibilidad;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Value("${GEMINI_API_KEY:}") private String clave;
    @Value("${GEMINI_MODEL:gemini-2.5-flash}") private String modelo;

    public AlternativasAsistenciaService(UsuarioRepository usuarios, InscripcionRepository inscripciones,
                                         CatalogoController catalogo, ObjectMapper json, DisponibilidadService disponibilidad) {
        this.usuarios = usuarios;
        this.inscripciones = inscripciones;
        this.catalogo = catalogo;
        this.json = json;
        this.disponibilidad = disponibilidad;
    }

    public record Eleccion(String id, String motivo) {}

    @SuppressWarnings("unchecked")
    public Map<String, Object> buscar(String sujeto, String actividadId, String motivo, Instant ahora) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(sujeto).or(() -> usuarios.findByCorreo(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        List<Inscripcion> mias = inscripciones.findByEstudianteId(usuario.getId());
        Inscripcion dejada = mias.stream().filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())
                && actividadId != null && actividadId.equals(i.getActividadId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No estás inscrito en esa actividad."));
        List<Map<String, Object>> publicadas = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        if (publicadas == null) publicadas = List.of();
        Map<String, Object> original = publicadas.stream().filter(a -> actividadId.equals(String.valueOf(a.get("id"))))
                .findFirst().orElseGet(() -> desdeInscripcion(dejada));
        boolean porHorario = !"otro".equalsIgnoreCase(motivo);
        LocalDateTime local = LocalDateTime.ofInstant(ahora, LIMA);
        List<Map<String, Object>> candidatas = candidatas(original, dejada, mias, publicadas, porHorario, local);
        // Con el horario de clases confirmado, tampoco se propone nada en horas de clase.
        var horario = disponibilidad.obtener(sujeto);
        if (horario.isConfirmado() && horario.getBloques() != null && !horario.getBloques().isEmpty()) {
            List<Inscripcion> otras = mias.stream().filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())
                    && !actividadId.equals(i.getActividadId())).toList();
            candidatas = AgendaInteligenteService.enHorasLibres(candidatas, horario.getBloques(), otras);
        }

        String origen = "reglas";
        Map<String, String> motivosIa = new HashMap<>();
        List<Map<String, Object>> ordenadas = candidatas;
        List<Eleccion> ia = elegirConIa(original, candidatas, porHorario);
        if (!ia.isEmpty()) {
            Map<String, Map<String, Object>> porId = new LinkedHashMap<>();
            candidatas.forEach(c -> porId.put(String.valueOf(c.get("id")), c));
            List<Map<String, Object>> elegidas = new ArrayList<>();
            for (Eleccion e : ia) {
                Map<String, Object> c = porId.remove(e.id());
                if (c == null) continue;
                elegidas.add(c);
                motivosIa.put(e.id(), e.motivo());
            }
            elegidas.addAll(porId.values()); // completar con las reglas si la IA eligió pocas
            ordenadas = elegidas;
            origen = "ia";
        }
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> c : ordenadas.subList(0, Math.min(MAXIMO, ordenadas.size()))) {
            Map<String, Object> item = new LinkedHashMap<>(c);
            String id = String.valueOf(c.get("id"));
            item.put("motivo", motivosIa.getOrDefault(id, motivoReglas(original, c, local)));
            item.put("etiquetas", etiquetas(original, c, local));
            item.put("inscripcionDirecta", !RecomendacionesAppService.porEquipos(c));
            item.remove("puntaje");
            salida.add(item);
        }
        boolean capitan = "capitan".equals(dejada.getRolEquipo());
        Map<String, Object> r = new LinkedHashMap<>();
        Map<String, Object> act = new LinkedHashMap<>();
        act.put("id", actividadId);
        act.put("nombre", texto(original.get("nombre"), texto(dejada.getNombreActividad(), "Actividad")));
        act.put("fecha", texto(original.get("fecha"), texto(dejada.getFecha(), "")));
        act.put("horaInicio", texto(original.get("horaInicio"), texto(dejada.getHoraInicio(), "")));
        act.put("horaFin", texto(original.get("horaFin"), texto(dejada.getHoraFin(), "")));
        act.put("ubicacion", texto(original.get("ubicacion"), ""));
        act.put("rolEquipo", dejada.getRolEquipo());
        r.put("actividad", act);
        r.put("motivo", porHorario ? "horario" : "otro");
        r.put("origen", salida.isEmpty() ? "sin_alternativas" : origen);
        r.put("puedeCambiar", !capitan);
        r.put("aviso", salida.isEmpty()
                ? "Por ahora no hay otras actividades con cupos que encajen en tu agenda."
                : capitan ? "Eres capitán: si cancelas, tu equipo sale de la competencia. Coordina con tu equipo antes de cambiarte."
                : "Ninguna se cruza con tus otras inscripciones. Si te cambias, liberamos tu cupo en «" + act.get("nombre") + "».");
        r.put("total", salida.size());
        r.put("alternativas", salida);
        return r;
    }

    /** Candidatas válidas ordenadas por las reglas (puntaje de parecido, luego fecha y hora). Se prueba directamente. */
    static List<Map<String, Object>> candidatas(Map<String, Object> original, Inscripcion dejada, List<Inscripcion> mias,
                                                List<Map<String, Object>> catalogo, boolean porHorario, LocalDateTime ahora) {
        var horarioOriginal = AgendaSugeridaService.horario(texto(original.get("fecha"), dejada.getFecha()),
                texto(original.get("horaInicio"), dejada.getHoraInicio()), texto(original.get("horaFin"), dejada.getHoraFin()));
        List<AgendaSugeridaService.Bloque> ocupadas = new ArrayList<>();
        Set<String> inscritas = new HashSet<>();
        for (Inscripcion i : mias) {
            if (i == null || !"inscrita".equalsIgnoreCase(i.getEstado())) continue;
            inscritas.add(String.valueOf(i.getActividadId()));
            if (i == dejada || String.valueOf(i.getActividadId()).equals(dejada.getActividadId())) continue; // queda libre
            var b = AgendaSugeridaService.horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin());
            if (b != null) ocupadas.add(b);
        }
        List<Map<String, Object>> aptas = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (Map<String, Object> a : catalogo) {
            if (a == null) continue;
            String id = texto(a.get("id"), "");
            if (id.isBlank() || inscritas.contains(id) || !vistas.add(id)) continue;
            if (!"programada".equalsIgnoreCase(texto(a.get("estado"), "programada"))) continue;
            if (a.get("cuposDisponibles") instanceof Number n && n.intValue() <= 0) continue;
            var h = AgendaSugeridaService.horario(texto(a.get("fecha"), ""), texto(a.get("horaInicio"), ""), texto(a.get("horaFin"), ""));
            if (h == null || !LocalDateTime.of(h.fecha(), h.inicio()).isAfter(ahora)) continue;
            if (ocupadas.stream().anyMatch(h::cruza)) continue;
            if (porHorario && horarioOriginal != null && horarioOriginal.cruza(h)) continue;
            Map<String, Object> c = new LinkedHashMap<>(a);
            int puntaje = ActividadesSimilaresService.puntuacion(original, a);
            if (horarioOriginal != null && h.fecha().equals(horarioOriginal.fecha())) puntaje += 1;
            c.put("puntaje", puntaje);
            aptas.add(c);
        }
        aptas.sort(Comparator.<Map<String, Object>>comparingInt(c -> -((Number) c.get("puntaje")).intValue())
                .thenComparing(c -> texto(c.get("fecha"), ""))
                .thenComparing(c -> texto(c.get("horaInicio"), "")));
        return aptas;
    }

    /** Explicación verificable cuando no hay IA. */
    static String motivoReglas(Map<String, Object> original, Map<String, Object> c, LocalDateTime ahora) {
        List<String> razones = new ArrayList<>();
        if (igual(original, c, "categoriaId")) razones.add("es de la misma categoría");
        else if (igual(original, c, "areaTecsup")) razones.add("es de la misma área");
        Set<String> comunes = palabras(original.get("nombre"));
        comunes.retainAll(palabras(c.get("nombre")));
        if (!comunes.isEmpty() && razones.isEmpty()) razones.add("tiene un tema parecido");
        String cuando = cuando(c, ahora);
        String base = razones.isEmpty() ? "Encaja en tu agenda" : "Se parece a la que dejas: " + String.join(" y ", razones);
        return base + (cuando.isBlank() ? "." : " y puedes ir " + cuando + ".");
    }

    static List<String> etiquetas(Map<String, Object> original, Map<String, Object> c, LocalDateTime ahora) {
        List<String> e = new ArrayList<>();
        if (igual(original, c, "categoriaId")) e.add("Misma categoría");
        else if (igual(original, c, "areaTecsup")) e.add("Misma área");
        String cuando = cuando(c, ahora);
        if (!cuando.isBlank()) e.add(Character.toUpperCase(cuando.charAt(0)) + cuando.substring(1));
        if (c.get("cuposDisponibles") instanceof Number n && n.intValue() > 0 && n.intValue() <= RecomendacionesAppService.CUPOS_POCOS)
            e.add(n.intValue() == 1 ? "Queda 1 cupo" : "Quedan " + n.intValue() + " cupos");
        if (RecomendacionesAppService.porEquipos(c)) e.add("Por equipos");
        return e;
    }

    /** «hoy a las 16:00», «mañana a las 10:00», «el martes 24 a las 15:00». */
    static String cuando(Map<String, Object> c, LocalDateTime ahora) {
        var h = AgendaSugeridaService.horario(texto(c.get("fecha"), ""), texto(c.get("horaInicio"), ""), texto(c.get("horaFin"), ""));
        if (h == null) return "";
        String dia = h.fecha().equals(ahora.toLocalDate()) ? "hoy"
                : h.fecha().equals(ahora.toLocalDate().plusDays(1)) ? "mañana" : "el " + h.fecha().format(DIA);
        return dia + " a las " + h.inicio();
    }

    private List<Eleccion> elegirConIa(Map<String, Object> original, List<Map<String, Object>> candidatas, boolean porHorario) {
        if (clave == null || clave.isBlank() || candidatas.isEmpty()) return List.of();
        try {
            List<Map<String, String>> opciones = candidatas.stream().limit(20).map(a -> Map.of(
                    "id", String.valueOf(a.get("id")), "nombre", texto(a.get("nombre"), ""),
                    "descripcion", texto(a.get("descripcion"), ""), "categoria", texto(a.get("categoria"), texto(a.get("categoriaId"), "")),
                    "fecha", texto(a.get("fecha"), ""), "inicio", texto(a.get("horaInicio"), ""))).toList();
            String prompt = "Un estudiante no puede asistir a la actividad " + json.writeValueAsString(Map.of(
                    "nombre", texto(original.get("nombre"), ""), "descripcion", texto(original.get("descripcion"), ""),
                    "fecha", texto(original.get("fecha"), ""), "inicio", texto(original.get("horaInicio"), "")))
                    + (porHorario ? " porque no puede en ese horario." : ".")
                    + " Elige hasta " + MAXIMO + " alternativas de esta lista, las más parecidas primero, y explica cada una"
                    + " en una frase corta en español dirigida al estudiante. Únicas opciones permitidas: "
                    + json.writeValueAsString(opciones) + ". Responde SOLO JSON {\"alternativas\":[{\"id\":\"id existente\","
                    + "\"motivo\":\"frase\"}]}. No inventes actividades, fechas ni horarios.";
            String payload = json.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2)));
            String seguro = modelo.matches("[A-Za-z0-9._-]+") ? modelo : "gemini-2.5-flash";
            HttpResponse<String> respuesta = GeminiCliente.enviar(http, seguro, clave, payload, Duration.ofSeconds(15));
            if (respuesta.statusCode() / 100 != 2) return List.of();
            String texto = json.readTree(respuesta.body()).path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            JsonNode lista = json.readTree(texto).path("alternativas");
            Set<String> permitidos = new HashSet<>();
            opciones.forEach(o -> permitidos.add(o.get("id")));
            List<Eleccion> elegidas = new ArrayList<>();
            Set<String> vistos = new HashSet<>();
            if (lista.isArray()) for (JsonNode n : lista) {
                String id = n.path("id").asText("");
                String motivo = n.path("motivo").asText("").strip();
                if (permitidos.contains(id) && vistos.add(id) && !motivo.isBlank() && elegidas.size() < MAXIMO)
                    elegidas.add(new Eleccion(id, motivo.length() > 220 ? motivo.substring(0, 220) : motivo));
            }
            return elegidas;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            return List.of(); // sin IA: se responde con las reglas
        }
    }

    private static Map<String, Object> desdeInscripcion(Inscripcion i) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", i.getActividadId());
        m.put("nombre", i.getNombreActividad());
        m.put("descripcion", i.getDescripcion());
        m.put("categoriaId", i.getCategoria());
        m.put("fecha", i.getFecha());
        m.put("horaInicio", i.getHoraInicio());
        m.put("horaFin", i.getHoraFin());
        return m;
    }

    private static boolean igual(Map<String, Object> a, Map<String, Object> b, String campo) {
        String x = texto(a.get(campo), ""), y = texto(b.get(campo), "");
        return !x.isBlank() && x.equalsIgnoreCase(y);
    }

    private static Set<String> palabras(Object valor) {
        Set<String> stop = Set.of("de", "la", "el", "del", "en", "para", "los", "las", "una", "torneo", "competencia",
                "actividad", "charla", "taller", "aplicada");
        Set<String> r = new java.util.LinkedHashSet<>();
        for (String p : RecomendacionesAppService.plano(String.valueOf(valor == null ? "" : valor)).split(" "))
            if (p.length() >= 4 && !stop.contains(p)) r.add(p);
        return r;
    }

    private static String texto(Object v, String porDefecto) {
        String s = v == null ? "" : String.valueOf(v).strip();
        return s.isEmpty() || "null".equals(s) ? porDefecto : s;
    }
}
