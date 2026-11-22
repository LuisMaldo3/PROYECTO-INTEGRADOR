package pe.tecsup.vynk.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
 * US-70: la programación completa de un día para la app. Reúne las actividades publicadas de esa fecha (con su
 * portada y su ficha de ubicación), marca en cuáles está inscrito el estudiante y calcula si cada una está por
 * empezar, en curso, finalizada o cancelada. También entrega los días que tienen actividades, para cambiar de día.
 */
@Service
public class ProgramacionDiaService {
    public static final String PROXIMA = "proxima";
    public static final String EN_CURSO = "en_curso";
    public static final String FINALIZADA = "finalizada";
    public static final String CANCELADA = "cancelada";

    private final CatalogoController catalogo;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public ProgramacionDiaService(CatalogoController catalogo, InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.catalogo = catalogo;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    /** @param fecha yyyy-MM-dd; vacía o null elige el día más útil (hoy, o el próximo con actividades). */
    public Map<String, Object> delDia(String sujeto, String fecha, Instant ahora) {
        String usuarioId = usuarioId(sujeto);
        LocalDate hoy = ahora.atZone(ProgramadorRecordatorios.LIMA).toLocalDate();
        Map<String, Integer> dias = catalogo.diasConActividades();
        String elegida = fecha == null || fecha.isBlank() ? diaInicial(dias.keySet(), hoy) : validar(fecha);

        Set<String> mias = new HashSet<>();
        for (Inscripcion i : inscripciones.findByEstudianteId(usuarioId))
            if ("inscrita".equalsIgnoreCase(i.getEstado())) mias.add(i.getActividadId());
        for (Inscripcion i : inscripciones.buscarPorIntegrante(usuarioId)) mias.add(i.getActividadId());

        List<Map<String, Object>> actividades = new ArrayList<>();
        for (Map<String, Object> a : catalogo.delDia(elegida)) {
            Map<String, Object> item = new LinkedHashMap<>(a);
            item.put("inscrito", mias.contains(String.valueOf(a.get("id"))));
            item.put("estadoDia", estadoDia(a, ahora));
            actividades.add(item);
        }
        List<Map<String, Object>> listaDias = new ArrayList<>();
        dias.forEach((d, total) -> listaDias.add(Map.of("fecha", d, "total", total)));

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("fecha", elegida);
        respuesta.put("hoy", hoy.toString());
        respuesta.put("dias", listaDias);
        respuesta.put("total", actividades.size());
        respuesta.put("actividades", actividades);
        return respuesta;
    }

    /** Hoy si tiene actividades; si no, el próximo día que tenga; si ya pasaron todos, el último; sin días, hoy. */
    static String diaInicial(Set<String> dias, LocalDate hoy) {
        String hoyTexto = hoy.toString();
        if (dias.contains(hoyTexto)) return hoyTexto;
        String ultimo = null;
        for (String d : new java.util.TreeSet<>(dias)) {
            if (d.compareTo(hoyTexto) > 0) return d;
            ultimo = d;
        }
        return ultimo != null ? ultimo : hoyTexto;
    }

    /** Estado de la actividad en este momento (hora de Perú). */
    static String estadoDia(Map<String, Object> a, Instant ahora) {
        String estado = String.valueOf(a.getOrDefault("estado", "programada"));
        if ("cancelada".equalsIgnoreCase(estado)) return CANCELADA;
        if ("finalizada".equalsIgnoreCase(estado)) return FINALIZADA;
        Instant inicio = Horario.de(a.get("fecha"), a.get("horaInicio"), a.get("horaFin")).comienzo();
        Instant fin = Horario.de(a.get("fecha"), a.get("horaFin"), null).comienzo();
        if (inicio == null || ahora.isBefore(inicio)) return PROXIMA;
        if (fin != null && !ahora.isBefore(fin)) return FINALIZADA;
        return EN_CURSO;
    }

    private static String validar(String fecha) {
        try {
            return LocalDate.parse(fecha.strip()).toString();
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha debe tener el formato AAAA-MM-DD");
        }
    }

    private String usuarioId(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(sujeto).or(() -> usuarios.findById(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario.getId();
    }
}
