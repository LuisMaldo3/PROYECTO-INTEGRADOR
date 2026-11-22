package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * US-76 T1: recomendaciones de IA preparadas para la app móvil ({@code GET /api/app/recomendaciones}).
 *
 * <ul>
 *   <li>Con horario confirmado se usan las recomendaciones que caben en las horas libres
 *       ({@link SugerenciasHuecosService}).</li>
 *   <li>Sin horario la app no queda vacía: se recomiendan por intereses y sin cruces con las inscripciones
 *       ({@link AgendaSugeridaService}) y se invita a subir el horario.</li>
 *   <li>Cada recomendación trae la actividad completa del catálogo (portada, lugar, modalidad) para abrir su
 *       detalle e inscribirse (T06), etiquetas cortas de «por qué» y si se puede inscribir en un toque.</li>
 *   <li>{@code vigenteHasta} le dice a la app hasta cuándo puede reutilizar la respuesta sin volver a pedirla.</li>
 * </ul>
 */
@Service
public class RecomendacionesAppService {
    static final Duration VIGENCIA = Duration.ofMinutes(15);
    static final int CUPOS_POCOS = 5;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final SugerenciasHuecosService huecos;
    private final AgendaSugeridaService agenda;
    private final InteresesService intereses;
    private final DisponibilidadService disponibilidad;

    public RecomendacionesAppService(SugerenciasHuecosService huecos, AgendaSugeridaService agenda,
                                     InteresesService intereses, DisponibilidadService disponibilidad) {
        this.huecos = huecos;
        this.agenda = agenda;
        this.intereses = intereses;
        this.disponibilidad = disponibilidad;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generar(String sujeto, Instant ahora) {
        Map<String, Object> base = huecos.generar(sujeto);
        boolean conHorario = Boolean.TRUE.equals(base.get("configurado"));
        List<String> misIntereses;
        String aviso;
        if (!conHorario) {
            base = agenda.generar(sujeto);
            Object guardados = base.get("intereses");
            misIntereses = guardados instanceof List<?> l ? (List<String>) l : List.of();
            boolean hay = base.get("actividades") instanceof List<?> l && !l.isEmpty();
            aviso = hay ? "Elegidas por tus intereses y sin cruces con tus inscripciones. Sube tu horario de clases "
                    + "para que además encajen en tus horas libres."
                    : "Todavía no hay actividades para recomendarte. Sube tu horario y revisa tus intereses.";
        } else {
            List<String> guardados = intereses.deUsuario(disponibilidad.usuarioId(sujeto)).getIntereses();
            misIntereses = guardados == null ? List.of() : guardados;
            aviso = String.valueOf(base.getOrDefault("aviso", ""));
        }
        List<Map<String, Object>> actividades = base.get("actividades") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        LocalDate hoy = LocalDate.ofInstant(ahora, LIMA);
        List<Map<String, Object>> recomendaciones = new ArrayList<>();
        for (Map<String, Object> a : actividades) recomendaciones.add(preparar(a, misIntereses, hoy));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("generadoEn", ahora.toString());
        r.put("vigenteHasta", ahora.plus(VIGENCIA).toString());
        r.put("origen", String.valueOf(base.getOrDefault("origen", "reglas")));
        r.put("conHorario", conHorario);
        r.put("aviso", aviso);
        r.put("intereses", misIntereses);
        r.put("total", recomendaciones.size());
        r.put("recomendaciones", recomendaciones);
        return r;
    }

    /** Copia de la actividad con lo que la app necesita para mostrarla e inscribirse. */
    static Map<String, Object> preparar(Map<String, Object> actividad, List<String> intereses, LocalDate hoy) {
        Map<String, Object> item = new LinkedHashMap<>(actividad);
        boolean equipos = porEquipos(actividad);
        Object cupos = actividad.get("cuposDisponibles");
        boolean sinCupos = cupos instanceof Number n && n.intValue() <= 0;
        item.put("modalidadInscripcion", equipos ? "equipos" : "individual");
        // Por equipos hay que poner el nombre del equipo o un código: eso se hace desde el detalle.
        item.put("inscripcionDirecta", !equipos && !sinCupos);
        String motivo = String.valueOf(actividad.getOrDefault("motivo", "")).strip();
        if (motivo.isEmpty() || "null".equals(motivo)) item.put("motivo", "Actividad disponible que encaja con tu agenda.");
        item.put("etiquetas", etiquetas(actividad, intereses, hoy));
        return item;
    }

    static boolean porEquipos(Map<String, Object> a) {
        return "competencia".equals(a.get("tipoActividad")) && "equipos".equals(a.get("modalidad"));
    }

    /** Hasta cuatro razones cortas y comprobables: interés, cuándo, hueco libre, cupos y modalidad. */
    static List<String> etiquetas(Map<String, Object> a, List<String> intereses, LocalDate hoy) {
        List<String> salida = new ArrayList<>();
        String interes = interesCoincidente(a, intereses);
        if (interes != null) salida.add("Te interesa: " + interes);
        try {
            LocalDate fecha = LocalDate.parse(String.valueOf(a.get("fecha")));
            if (fecha.equals(hoy)) salida.add("Hoy");
            else if (fecha.equals(hoy.plusDays(1))) salida.add("Mañana");
        } catch (RuntimeException ignorada) { /* sin fecha válida no se etiqueta el día */ }
        if (Boolean.TRUE.equals(a.get("cabeEnHorario")) && a.get("huecoInicio") != null && a.get("huecoFin") != null)
            salida.add("Libre de " + a.get("huecoInicio") + " a " + a.get("huecoFin"));
        if (a.get("cuposDisponibles") instanceof Number n && n.intValue() > 0 && n.intValue() <= CUPOS_POCOS) {
            String unidad = porEquipos(a) ? (n.intValue() == 1 ? "equipo" : "equipos") : (n.intValue() == 1 ? "cupo" : "cupos");
            salida.add((n.intValue() == 1 ? "Queda 1 " : "Quedan " + n.intValue() + " ") + unidad);
        }
        if (porEquipos(a)) salida.add("Por equipos");
        return salida.size() > 4 ? salida.subList(0, 4) : salida;
    }

    /** El primer interés del estudiante que aparece en la actividad, con el nombre que usaría él. */
    static String interesCoincidente(Map<String, Object> a, List<String> intereses) {
        if (intereses == null || intereses.isEmpty()) return null;
        String texto = plano(a.get("nombre") + " " + a.get("descripcion") + " " + a.get("categoriaId") + " "
                + a.get("categoria") + " " + a.get("areaTecsup"));
        for (String id : intereses) {
            if (id == null || id.isBlank()) continue;
            String tema = SugerenciasAsistenteService.temaDeInteres(id);
            String nombre = tema == null ? id.strip() : tema;
            if (contienePalabra(texto, plano(id)) || contienePalabra(texto, plano(nombre))) return nombre;
        }
        return null;
    }

    private static boolean contienePalabra(String texto, String buscado) {
        if (buscado.isBlank()) return false;
        // «ia» no debe coincidir con «industria»: se busca como palabra; los temas largos, como frase.
        return (" " + texto + " ").contains(" " + buscado + " ")
                || (buscado.length() >= 5 && texto.contains(buscado));
    }

    static String plano(String s) {
        String sinTildes = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
    }
}
