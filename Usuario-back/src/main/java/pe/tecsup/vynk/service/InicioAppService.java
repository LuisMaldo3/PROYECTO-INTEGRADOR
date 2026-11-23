package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.AgendaController;
import pe.tecsup.vynk.controller.AvisosPublicosController;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/**
 * US-81 T1: resumen de la pantalla de inicio en una sola llamada ({@code GET /api/app/inicio}).
 * <ul>
 *   <li><b>saludo</b> y <b>resumen</b>: cuántas actividades tengo (hoy, esta semana, en total) y equipos por confirmar.</li>
 *   <li><b>proximas</b>: mis próximas actividades (individuales y de equipo, una vez cada una), con portada y lugar.</li>
 *   <li><b>destacadas</b> (T07): actividades y competencias por venir en las que no estoy inscrito, priorizando
 *       competencias, las que tienen portada, las «Para ti» y las de últimos cupos. Siempre llevan portada: la publicada
 *       o, si no tiene, la app usa la ilustración del tema ({@code tienePortada=false}).</li>
 *   <li><b>avisos</b> (T06): los avisos recientes con su imagen lista para mostrar (incluidas las subidas o generadas,
 *       que antes no se veían en la app) y si son nuevos.</li>
 * </ul>
 * {@code vigenteHasta} indica hasta cuándo la app puede reutilizar el resumen.
 */
@Service
public class InicioAppService {
    static final Duration VIGENCIA = Duration.ofMinutes(2);
    static final int MAX_PROXIMAS = 6, MAX_DESTACADAS = 6, MAX_AVISOS = 5;
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d", Locale.forLanguageTag("es-PE"));

    private final UsuarioRepository usuarios;
    private final InscripcionRepository inscripciones;
    private final CatalogoController catalogo;
    private final AvisosPublicosController avisos;
    private final PersonalizacionCatalogo personalizacion;

    public InicioAppService(UsuarioRepository usuarios, InscripcionRepository inscripciones, CatalogoController catalogo,
                            AvisosPublicosController avisos, PersonalizacionCatalogo personalizacion) {
        this.usuarios = usuarios;
        this.inscripciones = inscripciones;
        this.catalogo = catalogo;
        this.avisos = avisos;
        this.personalizacion = personalizacion;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> resumen(String sujeto, Instant ahora) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(sujeto).or(() -> usuarios.findByCorreo(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // Igual que la agenda: una fila por actividad, con el estado del equipo y sin equipos que ya no existen.
        var sincronizadas = EquiposAgenda.sincronizar(inscripciones.findByEstudianteId(usuario.getId()),
                inscripciones::findById, inscripciones::findByEstudianteIdAndActividadId);
        Map<String, EquiposAgenda.Equipo> equipos = new HashMap<>();
        sincronizadas.entradas().forEach(e -> { if (e.equipo() != null) equipos.put(e.inscripcion().getActividadId(), e.equipo()); });
        List<Inscripcion> mias = new ArrayList<>(AgendaController.unir(
                sincronizadas.entradas().stream().map(EquiposAgenda.Entrada::inscripcion).toList(),
                inscripciones.buscarPorIntegrante(usuario.getId()),
                sincronizadas.sinEquipo().stream().map(Inscripcion::getActividadId).toList()).values());
        List<Map<String, Object>> publicadas = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        Map<String, Object> avisosPublicados = avisos.listar();
        List<Map<String, Object>> listaAvisos = avisosPublicados.get("avisos") instanceof List<?> l
                ? (List<Map<String, Object>>) l : List.of();
        return armar(usuario.getNombre(), mias, equipos, publicadas == null ? List.of() : publicadas,
                listaAvisos, personalizacion.intereses(sujeto), ahora);
    }

    /** Sin acceso a datos: se prueba directamente. */
    static Map<String, Object> armar(String nombre, List<Inscripcion> mias, Map<String, EquiposAgenda.Equipo> equipos,
                                     List<Map<String, Object>> catalogo, List<Map<String, Object>> avisos,
                                     List<String> intereses, Instant ahora) {
        LocalDateTime local = LocalDateTime.ofInstant(ahora, LIMA);
        LocalDate hoy = local.toLocalDate();
        Map<String, Map<String, Object>> porId = new HashMap<>();
        catalogo.forEach(a -> porId.put(String.valueOf(a.get("id")), a));

        // Mis próximas: solo las que siguen publicadas (no canceladas) y no terminaron.
        List<Map<String, Object>> proximas = new ArrayList<>();
        int deHoy = 0, deLaSemana = 0, porConfirmar = 0;
        for (Inscripcion i : mias) {
            Map<String, Object> a = porId.get(String.valueOf(i.getActividadId()));
            if (a == null) continue;
            var h = AgendaSugeridaService.horario(texto(a.get("fecha")), texto(a.get("horaInicio")), texto(a.get("horaFin")));
            if (h == null) continue;
            LocalDateTime ini = LocalDateTime.of(h.fecha(), h.inicio()), fin = LocalDateTime.of(h.fecha(), h.fin());
            if (!fin.isAfter(local)) continue;
            Map<String, Object> item = new LinkedHashMap<>(a);
            item.put("empiezaEn", ini.atZone(LIMA).toInstant().toString());
            item.put("terminaEn", fin.atZone(LIMA).toInstant().toString());
            boolean enCurso = !local.isBefore(ini);
            item.put("enCurso", enCurso);
            item.put("minutosParaEmpezar", enCurso ? 0L : Duration.between(local, ini).toMinutes());
            item.put("cuando", cuando(h.fecha(), hoy) + " · " + h.inicio());
            var e = equipos.get(String.valueOf(i.getActividadId()));
            if (e != null) {
                Map<String, Object> eq = new LinkedHashMap<>();
                eq.put("nombre", texto(e.nombre()).isBlank() ? "Tu equipo" : e.nombre());
                eq.put("confirmado", e.confirmado());
                eq.put("confirmados", e.confirmados());
                eq.put("integrantes", e.integrantes());
                item.put("equipo", eq);
                if (!e.confirmado()) porConfirmar++;
            } else item.put("equipo", null);
            proximas.add(item);
            if (h.fecha().equals(hoy)) deHoy++;
            if (h.fecha().isBefore(hoy.plusDays(7))) deLaSemana++;
        }
        proximas.sort(Comparator.comparing(m -> String.valueOf(m.get("empiezaEn"))));
        int total = proximas.size();

        // T07: destacadas con portada, en las que no estoy inscrito.
        var inscritas = mias.stream().map(i -> String.valueOf(i.getActividadId())).collect(java.util.stream.Collectors.toSet());
        List<Map<String, Object>> destacadas = new ArrayList<>();
        for (Map<String, Object> a : catalogo) {
            String id = String.valueOf(a.get("id"));
            String estado = texto(a.get("estado"));
            if (inscritas.contains(id) || (!estado.isBlank() && !"programada".equalsIgnoreCase(estado))) continue;
            if (a.get("cuposDisponibles") instanceof Number n && n.intValue() <= 0) continue;
            var h = AgendaSugeridaService.horario(texto(a.get("fecha")), texto(a.get("horaInicio")), texto(a.get("horaFin")));
            if (h == null || !LocalDateTime.of(h.fecha(), h.inicio()).isAfter(local)) continue;
            boolean competencia = "competencia".equals(a.get("tipoActividad"));
            boolean portada = !texto(a.get("portadaUrl")).isBlank();
            List<String> temas = PersonalizacionCatalogo.coincidencias(a, intereses);
            boolean ultimos = a.get("cuposDisponibles") instanceof Number n && n.intValue() <= RecomendacionesAppService.CUPOS_POCOS;
            int puntaje = (competencia ? 3 : 0) + (portada ? 2 : 0) + (temas.isEmpty() ? 0 : 2) + (ultimos ? 1 : 0);
            Map<String, Object> item = new LinkedHashMap<>(a);
            item.put("puntaje", puntaje);
            item.put("tienePortada", portada);
            item.put("paraTi", !temas.isEmpty());
            item.put("destacadaPor", competencia ? "Competencia" : !temas.isEmpty() ? "Para ti" : ultimos ? "Últimos cupos" : "Destacada");
            item.put("cuando", cuando(h.fecha(), hoy) + " · " + h.inicio());
            destacadas.add(item);
        }
        destacadas.sort(Comparator.<Map<String, Object>>comparingInt(m -> -((Number) m.get("puntaje")).intValue())
                .thenComparing(m -> texto(m.get("fecha")))
                .thenComparing(m -> texto(m.get("horaInicio"))));
        List<Map<String, Object>> elegidas = new ArrayList<>();
        for (var d : destacadas.subList(0, Math.min(MAX_DESTACADAS, destacadas.size()))) {
            d.remove("puntaje");
            elegidas.add(d);
        }

        // T06: avisos con imagen.
        List<Map<String, Object>> recientes = new ArrayList<>();
        for (Map<String, Object> av : avisos) {
            if (recientes.size() >= MAX_AVISOS) break;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", texto(av.get("id")));
            item.put("titulo", texto(av.get("titulo")).isBlank() ? "Comunicado" : texto(av.get("titulo")));
            item.put("mensaje", texto(av.get("mensaje")));
            item.put("publicadoEn", texto(av.get("publicadoEn")));
            Map<?, ?> portada = av.get("portada") instanceof Map<?, ?> m ? m : Map.of();
            item.put("imagenUrl", imagenAviso(portada));
            item.put("autor", texto(portada.get("autor")));
            item.put("creditoUrl", texto(portada.get("creditoUrl")));
            item.put("nuevo", esNuevo(texto(av.get("publicadoEn")), ahora));
            recientes.add(item);
        }

        String primerNombre = texto(nombre).split(" ")[0];
        Map<String, Object> saludo = new LinkedHashMap<>();
        saludo.put("nombre", primerNombre);
        saludo.put("fecha", hoy.toString());
        saludo.put("mensaje", mensaje(deHoy, total, proximas));

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("inscritas", total);
        resumen.put("hoy", deHoy);
        resumen.put("semana", deLaSemana);
        resumen.put("equiposPorConfirmar", porConfirmar);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("generadoEn", ahora.toString());
        r.put("vigenteHasta", ahora.plus(VIGENCIA).toString());
        r.put("saludo", saludo);
        r.put("resumen", resumen);
        r.put("proximas", new ArrayList<>(proximas.subList(0, Math.min(MAX_PROXIMAS, proximas.size()))));
        r.put("destacadas", elegidas);
        r.put("avisos", recientes);
        return r;
    }

    static String mensaje(int deHoy, int total, List<Map<String, Object>> proximas) {
        if (deHoy == 1) return "Hoy tienes 1 actividad.";
        if (deHoy > 1) return "Hoy tienes " + deHoy + " actividades.";
        if (total > 0) return "Tu próxima actividad es " + texto(proximas.get(0).get("cuando")).replace(" · ", " a las ") + ".";
        return "Descubre lo que pasa en el campus.";
    }

    /** «hoy», «mañana» o «el martes 24». */
    static String cuando(LocalDate fecha, LocalDate hoy) {
        if (fecha.equals(hoy)) return "hoy";
        if (fecha.equals(hoy.plusDays(1))) return "mañana";
        return "el " + fecha.format(DIA);
    }

    /** Imagen del aviso: las subidas o generadas se sirven desde /api/portadas/archivo; las externas solo si son https. */
    static String imagenAviso(Map<?, ?> portada) {
        String archivo = texto(portada.get("archivoId"));
        if (archivo.matches("[a-fA-F0-9]{24}")) return "/api/portadas/archivo/" + archivo;
        String url = texto(portada.get("url"));
        if (url.startsWith("/api/portadas/archivo/") || url.startsWith("https://")) return url;
        return null;
    }

    static boolean esNuevo(String iso, Instant ahora) {
        try { return Instant.parse(iso).isAfter(ahora.minus(Duration.ofDays(3))); }
        catch (RuntimeException e) { return false; }
    }

    private static String texto(Object v) {
        String s = v == null ? "" : String.valueOf(v).strip();
        return "null".equals(s) ? "" : s;
    }
}
