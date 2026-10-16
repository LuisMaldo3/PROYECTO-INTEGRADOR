package pe.tecsup.vynk.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Inscripcion;

/** Planifica sobre datos reales y comprueba los cruces aun si Gemini devuelve una propuesta incorrecta. */
@Service
public class AgendaSugeridaService {
    private final ContextoAgendaSugeridaService contexto;
    private final GeminiAgendaSugeridaService gemini;

    public AgendaSugeridaService(ContextoAgendaSugeridaService contexto, GeminiAgendaSugeridaService gemini) {
        this.contexto = contexto;
        this.gemini = gemini;
    }

    public record Bloque(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        public boolean cruza(Bloque otro) {
            return fecha.equals(otro.fecha) && inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
        }
    }

    public static Bloque horario(String fecha, String inicio, String fin) {
        try {
            Bloque b = new Bloque(LocalDate.parse(fecha), LocalTime.parse(inicio), LocalTime.parse(fin));
            return b.inicio().isBefore(b.fin()) ? b : null;
        } catch (RuntimeException e) { return null; }
    }
    private static String valor(Object v) { return v == null ? "" : String.valueOf(v); }

    public static List<Map<String, Object>> sinCruces(List<Inscripcion> inscritas,
                         List<Map<String, Object>> candidatas, Map<String, String> motivos, int maximo) {
        List<Bloque> ocupadas = new ArrayList<>();
        for (Inscripcion i : inscritas) {
            if ("cancelada".equalsIgnoreCase(i.getEstado()) || "cancelado".equalsIgnoreCase(i.getEstado())) continue;
            Bloque b = horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin());
            if (b != null) ocupadas.add(b);
        }
        Set<String> incluidos = new HashSet<>();
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map<String, Object> item : candidatas) {
            String id = valor(item.get("id"));
            Bloque b = horario(valor(item.get("fecha")), valor(item.get("horaInicio")), valor(item.get("horaFin")));
            if (id.isBlank() || b == null || incluidos.contains(id) || ocupadas.stream().anyMatch(b::cruza)) continue;
            incluidos.add(id);
            ocupadas.add(b);
            Map<String, Object> a = new LinkedHashMap<>(item);
            a.put("motivo", motivos.getOrDefault(id, "Actividad disponible compatible con tu horario."));
            resultado.add(a);
            if (resultado.size() >= maximo) break;
        }
        return resultado;
    }

    public Map<String, Object> generar(String usuarioId) {
        var datos = contexto.reunir(usuarioId);
        List<Map<String, Object>> candidatas = datos.disponibles();
        List<Map<String, String>> ocupadas = datos.inscritas().stream().map(i -> Map.of(
                "nombre", valor(i.getNombreActividad()), "fecha", valor(i.getFecha()),
                "inicio", valor(i.getHoraInicio()), "fin", valor(i.getHoraFin()))).toList();
        List<GeminiAgendaSugeridaService.Eleccion> decisiones = gemini.elegir(datos.intereses(), candidatas, ocupadas);
        Map<String, Map<String, Object>> porId = new HashMap<>();
        candidatas.forEach(a -> porId.put(valor(a.get("id")), a));
        List<Map<String, Object>> ordenadas = new ArrayList<>();
        Map<String, String> motivos = new HashMap<>();
        for (var d : decisiones) {
            Map<String, Object> a = porId.get(d.id());
            if (a == null) continue;
            ordenadas.add(a);
            if (!d.motivo().isBlank()) motivos.put(d.id(), d.motivo());
        }
        // Plan determinista, con relevancia local, cuando no hay IA o su respuesta es inviable.
        boolean conIa = !ordenadas.isEmpty();
        if (!conIa) {
            ordenadas.addAll(candidatas.stream().sorted(Comparator
                    .<Map<String, Object>>comparingInt(a -> -relevancia(a, datos.intereses()))
                    .thenComparing(a -> valor(a.get("fecha")))
                    .thenComparing(a -> valor(a.get("horaInicio")))).toList());
        }
        List<Map<String, Object>> propuesta = sinCruces(datos.inscritas(), ordenadas, motivos, 6);
        // Si la IA propuso solamente actividades incompatibles, intentar una solución por reglas.
        if (propuesta.isEmpty() && conIa) {
            conIa = false;
            propuesta = sinCruces(datos.inscritas(), candidatas, Map.of(), 6);
        }
        List<Map<String, String>> inscripciones = datos.inscritas().stream().map(i -> Map.of(
                "id", valor(i.getActividadId()), "nombre", valor(i.getNombreActividad()),
                "fecha", valor(i.getFecha()), "horaInicio", valor(i.getHoraInicio()),
                "horaFin", valor(i.getHoraFin()))).toList();
        String origen = propuesta.isEmpty() ? "sin_actividades" : conIa ? "ia" : "reglas";
        String aviso = propuesta.isEmpty() ? "No hay actividades disponibles compatibles con tu agenda actual."
                : conIa ? "Gemini priorizó actividades reales. Sus horarios fueron validados para evitar cruces."
                : "Propuesta calculada sin IA, a partir de intereses y horarios publicados.";
        return Map.of("origen", origen, "aviso", aviso, "intereses", datos.intereses(),
                "inscripciones", inscripciones, "actividades", propuesta, "total", propuesta.size());
    }

    static int relevancia(Map<String, Object> a, List<String> intereses) {
        String texto = (valor(a.get("nombre")) + " " + valor(a.get("descripcion")) + " "
                + valor(a.get("categoriaId"))).toLowerCase(Locale.ROOT);
        return (int) intereses.stream().filter(i -> i != null && !i.isBlank()
                && texto.contains(i.toLowerCase(Locale.ROOT))).count();
    }
}
