package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * US-73: filtros del catálogo por horario, categoría y área de Tecsup (T05). Lógica pura, sin base de datos.
 *
 * Horario: una franja (mañana, tarde, noche) y/o un rango «desde»–«hasta» (HH:mm) sobre la hora de inicio, y
 * opcionalmente una fecha. Categoría: su id («deportes»). Área: una de las cinco áreas, escrita con o sin tildes.
 */
public record FiltrosCatalogo(String franja, LocalTime desde, LocalTime hasta, LocalDate fecha, String categoria, String area) {
    public static final List<String> AREAS = List.of("Gestión y Producción", "Tecnología Digital",
            "Minería y Procesos Químicos", "Electricidad y Electrónica", "Mecánica");
    /** Franjas por hora de inicio: mañana antes de las 12:00, tarde de 12:00 a 17:59, noche desde las 18:00. */
    private static final Map<String, LocalTime[]> FRANJAS = new LinkedHashMap<>();
    static {
        FRANJAS.put("manana", new LocalTime[]{LocalTime.MIN, LocalTime.NOON});
        FRANJAS.put("tarde", new LocalTime[]{LocalTime.NOON, LocalTime.of(18, 0)});
        FRANJAS.put("noche", new LocalTime[]{LocalTime.of(18, 0), null});
    }

    /** Valida lo que llega por la URL; un valor mal escrito responde 400 en lugar de devolver todo. */
    public static FiltrosCatalogo desde(String franja, String desde, String hasta, String fecha, String categoria, String area) {
        String f = limpio(franja).toLowerCase();
        if ("mañana".equals(f)) f = "manana";
        if (!f.isEmpty() && !FRANJAS.containsKey(f)) error("La franja debe ser manana, tarde o noche");
        LocalTime d = hora(desde, "desde");
        LocalTime h = hora(hasta, "hasta");
        if (d != null && h != null && !d.isBefore(h)) error("La hora «desde» debe ser anterior a «hasta»");
        LocalDate dia = null;
        if (!limpio(fecha).isEmpty()) {
            try { dia = LocalDate.parse(limpio(fecha)); } catch (DateTimeParseException e) { error("La fecha debe tener el formato AAAA-MM-DD"); }
        }
        String c = limpio(categoria);
        if (c.length() > 80) error("Categoría inválida");
        String a = "";
        if (!limpio(area).isEmpty()) {
            a = AREAS.stream().filter(x -> sinTildes(x).equals(sinTildes(area))).findFirst().orElse(null);
            if (a == null) error("El área debe ser una de las cinco áreas de Tecsup");
        }
        return new FiltrosCatalogo(f, d, h, dia, c, a);
    }

    public boolean vacio() {
        return franja.isEmpty() && desde == null && hasta == null && fecha == null && categoria.isEmpty() && area.isEmpty();
    }

    public boolean cumple(Map<String, Object> a) {
        if (fecha != null && !fecha.toString().equals(String.valueOf(a.get("fecha")))) return false;
        if (!categoria.isEmpty() && !categoria.equals(String.valueOf(a.get("categoriaId")))) return false;
        if (!area.isEmpty() && !sinTildes(area).equals(sinTildes(String.valueOf(a.get("areaTecsup"))))) return false;
        if (franja.isEmpty() && desde == null && hasta == null) return true;
        LocalTime inicio;
        try { inicio = LocalTime.parse(String.valueOf(a.get("horaInicio"))); }
        catch (DateTimeParseException e) { return false; } // sin hora no se puede saber si está en el horario
        if (!franja.isEmpty()) {
            LocalTime[] r = FRANJAS.get(franja);
            if (inicio.isBefore(r[0]) || (r[1] != null && !inicio.isBefore(r[1]))) return false;
        }
        if (desde != null && inicio.isBefore(desde)) return false;
        return hasta == null || inicio.isBefore(hasta);
    }

    public List<Map<String, Object>> aplicar(List<Map<String, Object>> actividades) {
        return vacio() ? actividades : actividades.stream().filter(this::cumple).toList();
    }

    /**
     * Opciones para armar los filtros en la app, contadas sobre TODAS las actividades (así se puede cambiar de filtro
     * sin perder opciones): categorías con su nombre, las cinco áreas y las tres franjas.
     */
    public static Map<String, Object> opciones(List<Map<String, Object>> actividades) {
        Map<String, Object[]> categorias = new TreeMap<>();
        Map<String, Integer> areas = new LinkedHashMap<>();
        AREAS.forEach(a -> areas.put(a, 0));
        Map<String, Integer> franjas = new LinkedHashMap<>();
        FRANJAS.keySet().forEach(f -> franjas.put(f, 0));
        for (Map<String, Object> a : actividades) {
            String id = String.valueOf(a.get("categoriaId"));
            if (!id.isBlank() && !"null".equals(id)) {
                Object nombre = a.get("categoria");
                categorias.computeIfAbsent(id, k -> new Object[]{nombre == null ? k : String.valueOf(nombre), 0});
                categorias.get(id)[1] = (int) categorias.get(id)[1] + 1;
            }
            for (String area : AREAS)
                if (sinTildes(area).equals(sinTildes(String.valueOf(a.get("areaTecsup"))))) areas.merge(area, 1, Integer::sum);
            for (String f : FRANJAS.keySet())
                if (new FiltrosCatalogo(f, null, null, null, "", "").cumple(a)) franjas.merge(f, 1, Integer::sum);
        }
        List<Map<String, Object>> listaCategorias = new ArrayList<>();
        categorias.forEach((id, v) -> listaCategorias.add(Map.of("id", id, "nombre", v[0], "total", v[1])));
        List<Map<String, Object>> listaAreas = new ArrayList<>();
        areas.forEach((nombre, total) -> listaAreas.add(Map.of("nombre", nombre, "total", total)));
        List<Map<String, Object>> listaFranjas = new ArrayList<>();
        franjas.forEach((id, total) -> listaFranjas.add(Map.of("id", id, "total", total)));
        return Map.of("categorias", listaCategorias, "areas", listaAreas, "franjas", listaFranjas);
    }

    /** Lo que se aplicó, para que la app lo muestre igual que el servidor lo entendió. */
    public Map<String, Object> aplicados() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("franja", franja);
        m.put("desde", desde == null ? "" : desde.toString());
        m.put("hasta", hasta == null ? "" : hasta.toString());
        m.put("fecha", fecha == null ? "" : fecha.toString());
        m.put("categoria", categoria);
        m.put("area", area);
        return m;
    }

    private static LocalTime hora(String valor, String campo) {
        String v = limpio(valor);
        if (v.isEmpty()) return null;
        try { return LocalTime.parse(v); }
        catch (DateTimeParseException e) { error("La hora «" + campo + "» debe tener el formato HH:mm"); return null; }
    }

    private static String limpio(String v) { return v == null ? "" : v.strip(); }

    static String sinTildes(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase().strip();
    }

    private static void error(String mensaje) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
