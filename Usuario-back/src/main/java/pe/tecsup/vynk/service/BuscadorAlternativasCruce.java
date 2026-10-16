package pe.tecsup.vynk.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import pe.tecsup.vynk.model.Inscripcion;

/** Busca alternativas PUBLICADAS, con cupos, que no coincidan con ninguna inscripción activa. */
@Component
public class BuscadorAlternativasCruce {
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private static String texto(Object dato) { return dato == null ? "" : String.valueOf(dato).trim(); }

    private static boolean activa(Inscripcion i) {
        return i != null && !"cancelada".equalsIgnoreCase(i.getEstado())
                && !"cancelado".equalsIgnoreCase(i.getEstado());
    }

    /** Extremos contiguos (11:00-12:00 y 12:00-13:00) no se consideran cruce. */
    static boolean cruza(AgendaSugeridaService.Bloque a, AgendaSugeridaService.Bloque b) {
        return a != null && b != null && a.cruza(b);
    }

    static int relevancia(Map<String, Object> candidata, Inscripcion a, Inscripcion b) {
        String categoria = texto(candidata.get("categoriaId")).toLowerCase(Locale.ROOT);
        String nombre = (texto(candidata.get("nombre")) + " " + texto(candidata.get("descripcion"))).toLowerCase(Locale.ROOT);
        int puntaje = 0;
        for (Inscripcion original : List.of(a, b)) {
            String cat = texto(original.getCategoria()).toLowerCase(Locale.ROOT);
            if (!cat.isBlank() && (categoria.equals(cat) || nombre.contains(cat))) puntaje += 4;
        }
        if (texto(candidata.get("fecha")).equals(a.getFecha())) puntaje++;
        return puntaje;
    }

    public List<Map<String, Object>> buscar(CruceHorarioService.Cruce cruce,
                                            List<Inscripcion> inscripciones,
                                            List<Map<String, Object>> catalogo) {
        if (cruce == null || inscripciones == null || catalogo == null) return List.of();
        Inscripcion a = inscripciones.stream().filter(i -> activa(i) &&
                texto(i.getActividadId()).equals(cruce.actividadAId())).findFirst().orElse(null);
        Inscripcion b = inscripciones.stream().filter(i -> activa(i) &&
                texto(i.getActividadId()).equals(cruce.actividadBId())).findFirst().orElse(null);
        if (a == null || b == null) return List.of();

        List<AgendaSugeridaService.Bloque> ocupadas = new ArrayList<>();
        Set<String> idsInscritos = new HashSet<>();
        for (Inscripcion i : inscripciones) {
            if (!activa(i)) continue;
            idsInscritos.add(texto(i.getActividadId()));
            var bloque = AgendaSugeridaService.horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin());
            if (bloque != null) ocupadas.add(bloque);
        }
        LocalDate hoy = LocalDate.now(ZONA);
        List<Map<String, Object>> aptas = new ArrayList<>();
        Set<String> idsVistos = new HashSet<>();
        for (var actividad : catalogo) {
            if (actividad == null) continue;
            String id = texto(actividad.get("id"));
            if (id.isBlank() || idsInscritos.contains(id) || !idsVistos.add(id)) continue;
            if (!"programada".equalsIgnoreCase(texto(actividad.get("estado")))) continue;
            Object cupos = actividad.get("cuposDisponibles");
            if (cupos instanceof Number n && n.intValue() <= 0) continue;
            var horario = AgendaSugeridaService.horario(texto(actividad.get("fecha")),
                    texto(actividad.get("horaInicio")), texto(actividad.get("horaFin")));
            if (horario == null || horario.fecha().isBefore(hoy) || ocupadas.stream().anyMatch(horario::cruza)) continue;
            Map<String, Object> sugerencia = new LinkedHashMap<>(actividad);
            sugerencia.put("motivo", "Horario disponible y compatible con tus inscripciones actuales.");
            sugerencia.put("prioridad", relevancia(actividad, a, b));
            aptas.add(sugerencia);
        }
        aptas.sort(Comparator.<Map<String, Object>>comparingInt(m -> -((Number)m.get("prioridad")).intValue())
                .thenComparing(m -> texto(m.get("fecha")))
                .thenComparing(m -> texto(m.get("horaInicio"))));
        return aptas.stream().limit(4).map(m -> {
            Map<String, Object> limpia = new LinkedHashMap<>(m);
            limpia.remove("prioridad");
            return limpia;
        }).toList();
    }
}
