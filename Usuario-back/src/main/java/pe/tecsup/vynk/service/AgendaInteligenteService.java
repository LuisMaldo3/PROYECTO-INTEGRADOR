package pe.tecsup.vynk.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/**
 * US-77 T1: agenda inteligente para la app ({@code GET /api/app/agenda-inteligente}). Junta en una sola línea de tiempo
 * por día lo que el estudiante ya tiene (inscripciones individuales y de equipo, T06) y las actividades que la IA le
 * sugiere sin cruces ({@link AgendaSugeridaService}). Lo que ya terminó no se muestra.
 */
@Service
public class AgendaInteligenteService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Locale ES = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);

    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final AgendaSugeridaService sugerida;
    private final NombresUbicacion ubicaciones;
    private final DisponibilidadService disponibilidad;

    public AgendaInteligenteService(UsuarioRepository usuarios, InscripcionRepository inscripciones,
                                    AgendaSugeridaService sugerida, NombresUbicacion ubicaciones,
                                    DisponibilidadService disponibilidad) {
        this.usuarios = usuarios;
        this.inscripciones = inscripciones;
        this.sugerida = sugerida;
        this.ubicaciones = ubicaciones;
        this.disponibilidad = disponibilidad;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generar(String sujeto, Instant ahora) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(sujeto).or(() -> usuarios.findByCorreo(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        var equipos = EquiposAgenda.sincronizar(inscripciones.findByEstudianteId(usuario.getId()),
                inscripciones::findById, inscripciones::findByEstudianteIdAndActividadId);
        Map<String, Object> ia = sugerida.generar(sujeto);
        List<Map<String, Object>> sugeridas = ia.get("actividades") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        // Con el horario de clases confirmado, tampoco se sugiere nada que choque con una clase.
        var horario = disponibilidad.obtener(sujeto);
        List<BloqueOcupado> clases = horario.isConfirmado() && horario.getBloques() != null ? horario.getBloques() : List.of();
        if (!clases.isEmpty()) sugeridas = enHorasLibres(sugeridas, clases,
                equipos.entradas().stream().map(EquiposAgenda.Entrada::inscripcion).toList());
        Map<String, Object> r = armar(equipos, sugeridas, clases, ubicaciones.cargar(), ahora);
        r.put("conHorario", !clases.isEmpty());
        r.put("origen", String.valueOf(ia.getOrDefault("origen", "reglas")));
        r.put("aviso", String.valueOf(ia.getOrDefault("aviso", "")));
        r.put("iaEstado", String.valueOf(ia.getOrDefault("iaEstado", "")));
        return r;
    }

    /** Solo las sugeridas que caben enteras en una hora libre (sin clases ni inscripciones). */
    static List<Map<String, Object>> enHorasLibres(List<Map<String, Object>> sugeridas, List<BloqueOcupado> clases,
                                                   List<Inscripcion> inscritas) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> a : sugeridas) {
            try {
                LocalDate fecha = LocalDate.parse(String.valueOf(a.get("fecha")));
                if (SugerenciasHuecosService.cabe(a, SugerenciasHuecosService.huecos(fecha, clases, inscritas))) salida.add(a);
            } catch (RuntimeException ignorada) { /* sin fecha válida no se sugiere */ }
        }
        return salida;
    }

    /** Sin horario de clases. */
    static Map<String, Object> armar(EquiposAgenda.Resultado equipos, List<Map<String, Object>> sugeridas,
                                     Map<String, String> nombresUbicacion, Instant ahora) {
        return armar(equipos, sugeridas, List.of(), nombresUbicacion, ahora);
    }

    /**
     * Línea de tiempo por día, resumen y avisos de equipo. Las clases del horario confirmado se muestran como
     * referencia en los días que tienen actividades. Sin acceso a datos: se prueba directamente.
     */
    static Map<String, Object> armar(EquiposAgenda.Resultado equipos, List<Map<String, Object>> sugeridas,
                                     List<BloqueOcupado> clases, Map<String, String> nombresUbicacion, Instant ahora) {
        LocalDateTime local = LocalDateTime.ofInstant(ahora, LIMA);
        LocalDate hoy = local.toLocalDate();
        TreeMap<String, List<Map<String, Object>>> porDia = new TreeMap<>();
        List<String> avisos = new ArrayList<>();
        int inscritas = 0, enEquipo = 0, porConfirmar = 0, conSugerencia = 0;

        for (var entrada : equipos.entradas()) {
            Inscripcion i = entrada.inscripcion();
            var h = AgendaSugeridaService.horario(i.getFecha(), i.getHoraInicio(), i.getHoraFin());
            if (h == null || !LocalDateTime.of(h.fecha(), h.fin()).isAfter(local)) continue;
            Map<String, Object> b = bloque("inscrita", i.getActividadId(), i.getNombreActividad(), h,
                    NombresUbicacion.nombre(nombresUbicacion, i.getUbicacion()), i.getCategoria(), local);
            var e = entrada.equipo();
            b.put("modalidad", e == null ? "individual" : "equipo");
            if (e != null) {
                Map<String, Object> eq = new LinkedHashMap<>();
                eq.put("nombre", e.nombre() == null || e.nombre().isBlank() ? "Tu equipo" : e.nombre());
                eq.put("rol", e.rol());
                eq.put("integrantes", e.integrantes());
                eq.put("confirmados", e.confirmados());
                eq.put("confirmado", e.confirmado());
                b.put("equipo", eq);
                enEquipo++;
                if (!e.confirmado()) {
                    porConfirmar++;
                    avisos.add(avisoEquipo(String.valueOf(eq.get("nombre")), i.getNombreActividad(), e));
                }
            } else b.put("equipo", null);
            porDia.computeIfAbsent(h.fecha().toString(), k -> new ArrayList<>()).add(b);
            inscritas++;
        }
        for (Inscripcion huerfana : equipos.sinEquipo()) {
            var h = AgendaSugeridaService.horario(huerfana.getFecha(), huerfana.getHoraInicio(), huerfana.getHoraFin());
            if (h != null && !LocalDateTime.of(h.fecha(), h.fin()).isAfter(local)) continue;
            avisos.add("El equipo de «" + texto(huerfana.getNombreActividad(), "una competencia")
                    + "» ya no está activo, así que no ocupa tu agenda. Únete a otro equipo si quieres participar.");
        }
        for (Map<String, Object> a : sugeridas) {
            var h = AgendaSugeridaService.horario(texto(a.get("fecha"), ""), texto(a.get("horaInicio"), ""),
                    texto(a.get("horaFin"), ""));
            if (h == null || !LocalDateTime.of(h.fecha(), h.inicio()).isAfter(local)) continue;
            String lugar = a.get("ubicacion") != null && !texto(a.get("ubicacion"), "").isBlank()
                    ? texto(a.get("ubicacion"), "") : NombresUbicacion.nombre(nombresUbicacion, texto(a.get("ubicacionId"), ""));
            Map<String, Object> b = bloque("sugerida", texto(a.get("id"), ""), texto(a.get("nombre"), "Actividad"), h,
                    lugar, texto(a.get("categoria"), texto(a.get("categoriaId"), "")), local);
            boolean equipos2 = RecomendacionesAppService.porEquipos(a);
            b.put("modalidad", equipos2 ? "equipo" : "individual");
            b.put("equipo", null);
            b.put("motivo", texto(a.get("motivo"), "Actividad compatible con tu agenda."));
            Object cupos = a.get("cuposDisponibles");
            b.put("inscripcionDirecta", !equipos2 && !(cupos instanceof Number n && n.intValue() <= 0));
            porDia.computeIfAbsent(h.fecha().toString(), k -> new ArrayList<>()).add(b);
            conSugerencia++;
        }

        for (var entrada : porDia.entrySet()) {
            LocalDate fecha = LocalDate.parse(entrada.getKey());
            for (BloqueOcupado c : clases) {
                if (c.dia() != fecha.getDayOfWeek().getValue()) continue;
                var h = AgendaSugeridaService.horario(fecha.toString(), c.inicio(), c.fin());
                if (h == null || !LocalDateTime.of(h.fecha(), h.fin()).isAfter(local)) continue;
                Map<String, Object> b = bloque("clase", "", texto(c.nombre(), "Clases"), h, "", "", local);
                b.put("ubicacion", "");
                b.put("modalidad", "individual");
                b.put("equipo", null);
                entrada.getValue().add(b);
            }
        }
        List<Map<String, Object>> dias = new ArrayList<>();
        porDia.forEach((fecha, bloques) -> {
            bloques.sort(Comparator.comparing((Map<String, Object> b) -> String.valueOf(b.get("horaInicio")))
                    .thenComparing(b -> switch (String.valueOf(b.get("tipo"))) { case "clase" -> 0; case "inscrita" -> 1; default -> 2; }));
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("fecha", fecha);
            d.put("etiqueta", etiquetaDia(LocalDate.parse(fecha), hoy));
            d.put("bloques", bloques);
            dias.add(d);
        });

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("inscritas", inscritas);
        resumen.put("sugeridas", conSugerencia);
        resumen.put("enEquipo", enEquipo);
        resumen.put("equiposPorConfirmar", porConfirmar);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("generadoEn", ahora.toString());
        r.put("resumen", resumen);
        r.put("avisos", avisos);
        r.put("dias", dias);
        return r;
    }

    private static Map<String, Object> bloque(String tipo, String id, String nombre, AgendaSugeridaService.Bloque h,
                                              String lugar, String categoria, LocalDateTime ahora) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("tipo", tipo);
        b.put("actividadId", id);
        b.put("nombre", texto(nombre, "Actividad"));
        b.put("fecha", h.fecha().toString());
        b.put("horaInicio", h.inicio().toString());
        b.put("horaFin", h.fin().toString());
        b.put("ubicacion", texto(lugar, NombresUbicacion.POR_CONFIRMAR));
        b.put("categoria", texto(categoria, ""));
        LocalDateTime ini = LocalDateTime.of(h.fecha(), h.inicio()), fin = LocalDateTime.of(h.fecha(), h.fin());
        b.put("enCurso", !ahora.isBefore(ini) && ahora.isBefore(fin));
        return b;
    }

    static String avisoEquipo(String equipo, String actividad, EquiposAgenda.Equipo e) {
        String act = texto(actividad, "tu competencia");
        if (e.integrantes() < 2)
            return "Tu equipo «" + equipo + "» en «" + act + "» necesita al menos 2 integrantes para quedar confirmado.";
        int faltan = e.pendientes();
        return "Tu equipo «" + equipo + "» en «" + act + "» tiene " + e.confirmados() + " de " + e.integrantes()
                + " integrantes confirmados; " + (faltan == 1 ? "falta 1." : "faltan " + faltan + ".");
    }

    static String etiquetaDia(LocalDate fecha, LocalDate hoy) {
        String largo = fecha.format(DIA);
        if (fecha.equals(hoy)) return "Hoy · " + largo;
        if (fecha.equals(hoy.plusDays(1))) return "Mañana · " + largo;
        return Character.toUpperCase(largo.charAt(0)) + largo.substring(1);
    }

    private static String texto(Object v, String porDefecto) {
        String s = v == null ? "" : String.valueOf(v).strip();
        return s.isEmpty() || "null".equals(s) ? porDefecto : s;
    }
}
