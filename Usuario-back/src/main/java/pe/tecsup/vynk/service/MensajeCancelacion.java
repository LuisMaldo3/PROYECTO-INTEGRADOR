package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Contenido de la notificación de cancelación: viaja como «datos» para que la app la muestre y actualice la agenda. */
public final class MensajeCancelacion {
    private MensajeCancelacion() {}

    public static Map<String, String> datos(String actividadId, String nombreActividad, Horario horario, String lugar) {
        String nombre = recortar(nombreActividad, 80, "Tu actividad");
        String cuando = horario.completo() ? horario.paraMostrar() : "";
        String donde = lugar == null || NombresUbicacion.POR_CONFIRMAR.equals(lugar) ? "" : recortar(lugar, 60, "");
        String detalle = String.join(" · ", java.util.stream.Stream.of(cuando, donde).filter(s -> !s.isEmpty()).toList());
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("tipo", pe.tecsup.vynk.model.AvisoPush.CANCELACION);
        datos.put("actividadId", actividadId);
        datos.put("titulo", "Se canceló " + nombre);
        datos.put("detalle", detalle);
        datos.put("cuerpo", (detalle.isEmpty() ? "La actividad" : "La actividad del " + detalle)
                + " fue cancelada. Ya no está en tu agenda.");
        return datos;
    }

    static String recortar(String texto, int maximo, String porDefecto) {
        String limpio = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || "null".equals(limpio)) return porDefecto;
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 1) + "…";
    }
}
