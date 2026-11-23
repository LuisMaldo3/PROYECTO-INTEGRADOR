package pe.tecsup.vynk.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.config.InteresesIniciales;
import pe.tecsup.vynk.model.Interes;

/**
 * US-75 T01: preguntas sugeridas para el chat del celular, armadas con datos del estudiante y escritas como las
 * entiende el asistente: «¿Dónde se realiza …?», «¿Qué requisitos tiene …?» y «¿Qué actividades de … hay mañana?».
 * Tocar una la envía tal cual, así el estudiante no tiene que escribir en el teclado del celular.
 */
@Service
public class SugerenciasAsistenteService {
    static final int MAXIMO = 5;
    static final List<String> GENERALES = List.of(
            "¿Qué actividades hay hoy?",
            "¿Qué actividades hay mañana?",
            "¿Qué deportes hay después de las 3?",
            "¿Dónde se realizará una actividad?");

    private final MiProximaActividadService miProxima;
    private final ProximasActividadesService proximas;
    private final InteresesService intereses;
    private final DisponibilidadService disponibilidad;

    public SugerenciasAsistenteService(MiProximaActividadService miProxima, ProximasActividadesService proximas,
                                       InteresesService intereses, DisponibilidadService disponibilidad) {
        this.miProxima = miProxima;
        this.proximas = proximas;
        this.intereses = intereses;
        this.disponibilidad = disponibilidad;
    }

    public Map<String, Object> sugerir(String sujeto, Instant ahora) {
        String usuarioId = disponibilidad.usuarioId(sujeto);
        String mia = null;
        String proxima = null;
        Object actividad = miProxima.obtener(sujeto, ahora).get("actividad");
        if (actividad instanceof Map<?, ?> a) mia = String.valueOf(a.get("nombre"));
        Object lista = proximas.proximas(sujeto, 3, ahora).get("actividades");
        if (lista instanceof List<?> l)
            for (Object o : l)
                if (o instanceof Map<?, ?> a && !String.valueOf(a.get("nombre")).equals(mia)) { proxima = String.valueOf(a.get("nombre")); break; }
        List<String> suyos = intereses.deUsuario(usuarioId).getIntereses();
        return Map.of("sugerencias", armar(mia, proxima, suyos == null ? List.of() : suyos));
    }

    /** Lógica pura: personalizadas primero, luego generales, sin repetir y como máximo cinco. */
    static List<String> armar(String miProxima, String otraProxima, List<String> interesesUsuario) {
        Set<String> salida = new LinkedHashSet<>();
        String mia = nombreLimpio(miProxima);
        String otra = nombreLimpio(otraProxima);
        if (mia != null) salida.add("¿Dónde se realiza " + mia + "?");
        if (otra != null) salida.add("¿Qué requisitos tiene " + otra + "?");
        else if (mia != null) salida.add("¿Qué requisitos tiene " + mia + "?");
        for (String id : interesesUsuario) {
            String tema = temaDeInteres(id);
            if (tema != null) { salida.add("¿Qué actividades de " + tema + " hay mañana?"); break; }
        }
        for (String g : GENERALES) { if (salida.size() >= MAXIMO) break; salida.add(g); }
        return new ArrayList<>(salida).subList(0, Math.min(MAXIMO, salida.size()));
    }

    /** Nombre usable en una pregunta: sin comillas ni signos raros y no demasiado largo. */
    static String nombreLimpio(String nombre) {
        if (nombre == null) return null;
        String limpio = nombre.replaceAll("[\"«»¿?¡!]", "").replaceAll("\\s+", " ").strip();
        if (limpio.isEmpty() || "null".equals(limpio)) return null;
        return limpio.length() > 60 ? limpio.substring(0, 60).strip() : limpio;
    }

    /** «deportes» → «deportes»; «ia» → «inteligencia artificial»: el tema con el nombre que usaría el estudiante. */
    public static String temaDeInteres(String id) {
        if (id == null || id.isBlank()) return null;
        for (Interes i : InteresesIniciales.CATALOGO) {
            if (!i.getId().equals(id)) continue;
            return switch (id) {
                case "ia" -> "inteligencia artificial";
                case "teatro" -> "teatro";
                case "libros" -> "libros";
                case "concursos" -> "concursos";
                default -> i.getNombre().split(" ")[0].toLowerCase();
            };
        }
        return null;
    }
}
