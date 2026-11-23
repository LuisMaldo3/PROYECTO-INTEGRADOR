package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.repository.UsuarioRepository;

/**
 * US-80 T1: el catálogo ordenado según los intereses del estudiante. Cada actividad se marca con {@code paraTi}
 * (coincide con al menos un interés), {@code afinidad} (con cuántos) y {@code motivoParaTi} («Te interesa: deportes»).
 * Con orden por intereses, primero van las «Para ti» (más afinidad primero) y después el resto; dentro de cada grupo se
 * conserva el orden por fecha y hora. El interés se busca como palabra y sin tildes («ia» no es «industria»).
 */
@Service
public class PersonalizacionCatalogo {
    private final InteresesService intereses;
    private final UsuarioRepository usuarios;

    public PersonalizacionCatalogo(InteresesService intereses, UsuarioRepository usuarios) {
        this.intereses = intereses;
        this.usuarios = usuarios;
    }

    /** Intereses guardados del estudiante; vacío si no eligió o no se pudo leer (el catálogo no debe fallar). */
    public List<String> intereses(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) return List.of();
        try {
            var usuario = usuarios.findById(sujeto).or(() -> usuarios.findByCorreo(sujeto)).orElse(null);
            if (usuario == null) return List.of();
            List<String> guardados = intereses.deUsuario(usuario.getId()).getIntereses();
            return guardados == null ? List.of() : guardados;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** Nombres de los intereses que coinciden con la actividad, en el orden en que los eligió el estudiante. */
    public static List<String> coincidencias(Map<String, Object> actividad, List<String> intereses) {
        List<String> nombres = new ArrayList<>();
        if (intereses == null) return nombres;
        for (String id : intereses) {
            String nombre = RecomendacionesAppService.interesCoincidente(actividad, List.of(id == null ? "" : id));
            if (nombre != null && !nombres.contains(nombre)) nombres.add(nombre);
        }
        return nombres;
    }

    /** Copia de las actividades marcadas y, si {@code porIntereses}, ordenadas con las «Para ti» primero. */
    public static List<Map<String, Object>> ordenar(List<Map<String, Object>> actividades, List<String> intereses,
                                                    boolean porIntereses) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> a : actividades) {
            Map<String, Object> item = new LinkedHashMap<>(a);
            List<String> temas = coincidencias(a, intereses);
            item.put("paraTi", !temas.isEmpty());
            item.put("afinidad", temas.size());
            item.put("motivoParaTi", temas.isEmpty() ? null : "Te interesa: " + unir(temas));
            salida.add(item);
        }
        // sort es estable: dentro de cada nivel de afinidad se mantiene el orden por fecha y hora.
        if (porIntereses) salida.sort(Comparator.comparingInt(m -> -((Number) m.get("afinidad")).intValue()));
        return salida;
    }

    /** «deportes», «deportes y música», «deportes, música y teatro». */
    static String unir(List<String> temas) {
        if (temas.size() == 1) return temas.get(0);
        return String.join(", ", temas.subList(0, temas.size() - 1)) + " y " + temas.get(temas.size() - 1);
    }
}
