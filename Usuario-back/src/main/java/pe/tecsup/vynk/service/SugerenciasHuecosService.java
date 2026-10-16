package pe.tecsup.vynk.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.model.Inscripcion;

/** El cálculo de huecos se ejecuta en Java; Gemini solo ordena IDs previamente filtrados. */
@Service
public class SugerenciasHuecosService {
    private final ContextoAgendaSugeridaService contexto;
    private final DisponibilidadService disponibilidad;
    private final GeminiAgendaSugeridaService gemini;
    public SugerenciasHuecosService(ContextoAgendaSugeridaService contexto, DisponibilidadService disponibilidad, GeminiAgendaSugeridaService gemini) {
        this.contexto = contexto; this.disponibilidad = disponibilidad; this.gemini = gemini;
    }
    public record Franja(LocalTime inicio, LocalTime fin) {}
    private static final LocalTime ABRE = LocalTime.of(8, 0), CIERRA = LocalTime.of(21, 0);
    public static List<Franja> huecos(LocalDate fecha, List<BloqueOcupado> clases, List<Inscripcion> inscripciones) {
        List<Franja> ocupadas = new ArrayList<>();
        int dia = fecha.getDayOfWeek().getValue();
        for (var b : clases) if (b.dia() == dia) {
            try { ocupadas.add(new Franja(LocalTime.parse(b.inicio()), LocalTime.parse(b.fin()))); }
            catch (RuntimeException ignored) {}
        }
        for (var i : inscripciones) if (fecha.toString().equals(i.getFecha())) {
            try { ocupadas.add(new Franja(LocalTime.parse(i.getHoraInicio()), LocalTime.parse(i.getHoraFin()))); }
            catch (RuntimeException ignored) {}
        }
        ocupadas.sort(Comparator.comparing(Franja::inicio));
        List<Franja> libres = new ArrayList<>();
        LocalTime actual = ABRE;
        for (Franja o : ocupadas) {
            if (o.inicio().isAfter(actual)) libres.add(new Franja(actual, o.inicio().isBefore(CIERRA) ? o.inicio() : CIERRA));
            if (o.fin().isAfter(actual)) actual = o.fin();
            if (!actual.isBefore(CIERRA)) break;
        }
        if (actual.isBefore(CIERRA)) libres.add(new Franja(actual, CIERRA));
        return libres.stream().filter(f -> f.inicio().isBefore(f.fin())).toList();
    }
    public static boolean cabe(Map<String, Object> a, List<Franja> huecos) {
        try {
            LocalTime ini = LocalTime.parse(String.valueOf(a.get("horaInicio")));
            LocalTime fin = LocalTime.parse(String.valueOf(a.get("horaFin")));
            return ini.isBefore(fin) && huecos.stream().anyMatch(h -> !ini.isBefore(h.inicio()) && !fin.isAfter(h.fin()));
        } catch (RuntimeException e) { return false; }
    }
    public Map<String, Object> generar(String usuario) {
        var configuracion = disponibilidad.obtener(usuario);
        if (!configuracion.isConfirmado()) return Map.of("configurado", false, "origen", "sin_horario", "aviso", "Sube tu horario y confirma las horas detectadas para recibir recomendaciones.", "actividades", List.of(), "huecos", List.of());
        var datos = contexto.reunir(usuario);
        Map<LocalDate, List<Franja>> porFecha = new LinkedHashMap<>();
        List<Map<String, Object>> elegibles = new ArrayList<>();
        for (var a : datos.disponibles()) {
            LocalDate fecha;
            try { fecha = LocalDate.parse(String.valueOf(a.get("fecha"))); }
            catch (RuntimeException e) { continue; }
            List<Franja> huecos = porFecha.computeIfAbsent(fecha, d -> huecos(d, configuracion.getBloques(), datos.inscritas()));
            if (cabe(a, huecos)) elegibles.add(a);
        }
        var ocupadas = datos.inscritas().stream().map(i -> Map.of("fecha", String.valueOf(i.getFecha()), "inicio", String.valueOf(i.getHoraInicio()), "fin", String.valueOf(i.getHoraFin()))).toList();
        var selecciones = gemini.elegir(datos.intereses(), elegibles, ocupadas);
        Map<String, Map<String, Object>> porId = elegibles.stream().collect(Collectors.toMap(a -> String.valueOf(a.get("id")), a -> a, (a,b)->a));
        List<Map<String, Object>> prioridad = new ArrayList<>();
        Map<String, String> motivos = new HashMap<>();
        for (var eleccion : selecciones) {
            var actividad = porId.get(eleccion.id());
            if (actividad != null) { prioridad.add(actividad); motivos.put(eleccion.id(), eleccion.motivo()); }
        }
        boolean usaIa = !prioridad.isEmpty();
        if (!usaIa) prioridad.addAll(elegibles.stream().sorted(Comparator
            .<Map<String, Object>>comparingInt(a -> -AgendaSugeridaService.relevancia(a, datos.intereses()))
            .thenComparing(a -> String.valueOf(a.get("fecha")))).toList());
        var elegidas = AgendaSugeridaService.sinCruces(datos.inscritas(), prioridad, motivos, 6);
        if (elegidas.isEmpty() && usaIa) {
            usaIa = false;
            elegidas = AgendaSugeridaService.sinCruces(datos.inscritas(), elegibles, Map.of(), 6);
        }
        List<Map<String, Object>> salida = new ArrayList<>();
        for (var a : elegidas) {
            var fecha = LocalDate.parse(String.valueOf(a.get("fecha")));
            var ini = LocalTime.parse(String.valueOf(a.get("horaInicio")));
            var fin = LocalTime.parse(String.valueOf(a.get("horaFin")));
            var hueco = porFecha.get(fecha).stream().filter(h -> !ini.isBefore(h.inicio()) && !fin.isAfter(h.fin())).findFirst().orElseThrow();
            var item = new LinkedHashMap<>(a);
            item.put("huecoInicio", hueco.inicio().toString());
            item.put("huecoFin", hueco.fin().toString());
            salida.add(item);
        }
        List<Map<String, String>> intervalos = new ArrayList<>();
        porFecha.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(25).forEach(ent -> {
            for (Franja h : ent.getValue()) if (intervalos.size() < 70)
                intervalos.add(Map.of("fecha", ent.getKey().toString(), "inicio", h.inicio().toString(), "fin", h.fin().toString()));
        });
        return Map.of("configurado", true, "origen", salida.isEmpty() ? "sin_actividades" : usaIa ? "ia" : "reglas",
            "aviso", "Disponibilidad semanal confirmada. Franja de sugerencias: 08:00 a 21:00 (hora de Lima). Verifica tus compromisos adicionales.",
            "actividades", salida, "huecos", intervalos);
    }
}
