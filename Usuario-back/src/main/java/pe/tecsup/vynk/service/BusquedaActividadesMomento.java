package pe.tecsup.vynk.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Luis: consulta fechas y horarios reales del catálogo, sin inventar disponibilidad. */
public final class BusquedaActividadesMomento {
    private BusquedaActividadesMomento() {}

    public static List<Map<String, Object>> buscar(List<Map<String, Object>> catalogo, LocalDate fecha,
                                                    LocalTime momento, LocalTime hasta) {
        if (catalogo == null || fecha == null) return List.of();
        return catalogo.stream()
                .filter(a -> "programada".equalsIgnoreCase(texto(a, "estado", "programada")))
                .filter(a -> texto(a, "fecha", "").startsWith(fecha.toString()))
                .filter(a -> cupos(a))
                .filter(a -> horarioCoincide(a, momento, hasta))
                .sorted(Comparator.comparing((Map<String, Object> a) -> texto(a, "horaInicio", ""))
                        .thenComparing(a -> texto(a, "nombre", "")))
                .toList();
    }

    static boolean horarioCoincide(Map<String, Object> actividad, LocalTime momento, LocalTime hasta) {
        if (momento == null) return true;
        LocalTime inicio = hora(actividad.get("horaInicio"));
        LocalTime fin = hora(actividad.get("horaFin"));
        if (inicio == null || fin == null || !fin.isAfter(inicio)) return false;
        // Intervalos semiabiertos: una actividad de 14:00 a 15:00 NO ocurre a las 15:00.
        if (hasta == null) return !momento.isBefore(inicio) && momento.isBefore(fin);
        return inicio.isBefore(hasta) && fin.isAfter(momento);
    }

    private static boolean cupos(Map<String, Object> actividad) {
        Object libres = actividad.get("cuposDisponibles");
        if (libres instanceof Number n) return n.intValue() > 0;
        if (libres == null) return true; // Cupos sin publicar: no afirmar disponibilidad.
        try { return Integer.parseInt(String.valueOf(libres)) > 0; }
        catch (NumberFormatException ex) { return true; }
    }

    private static LocalTime hora(Object valor) {
        if (valor == null) return null;
        try {
            String texto = String.valueOf(valor).trim();
            // Formato HH:mm, HH:mm:ss o HH:mm:ss.SSS (sin asumir zona horaria).
            return LocalTime.parse(texto);
        } catch (DateTimeParseException ex) { return null; }
    }

    private static String texto(Map<String, Object> a, String clave, String defecto) {
        Object valor = a.get(clave);
        return valor == null ? defecto : String.valueOf(valor).trim();
    }
}
