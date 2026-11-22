package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import pe.tecsup.vynk.model.AvisoPush;

/** Contenido de la notificación de recomendación: viaja como «datos» para que la app la muestre y abra la actividad. */
public final class MensajeRecomendacion {
    private MensajeRecomendacion() {}

    /** @param actividad una actividad del catálogo (ya trae «ubicacion» con el nombre del lugar). */
    public static Map<String, String> datos(Map<String, Object> actividad, String motivo) {
        String nombre = MensajeCancelacion.recortar(String.valueOf(actividad.get("nombre")), 80, "una actividad");
        String cuando = Horario.de(actividad.get("fecha"), actividad.get("horaInicio"), actividad.get("horaFin")).paraMostrar();
        Object ubicacion = actividad.get("ubicacion");
        String lugar = ubicacion == null || NombresUbicacion.POR_CONFIRMAR.equals(ubicacion) ? ""
                : MensajeCancelacion.recortar(String.valueOf(ubicacion), 60, "");
        String detalle = String.join(" · ", Stream.of(cuando, lugar).filter(s -> !s.isEmpty()).toList());
        String razon = motivo == null ? "" : motivo.strip();
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("tipo", AvisoPush.RECOMENDACION);
        datos.put("actividadId", String.valueOf(actividad.get("id")));
        datos.put("titulo", "Te puede interesar: " + nombre);
        datos.put("detalle", detalle);
        datos.put("motivo", razon);
        datos.put("cuerpo", detalle + (razon.isEmpty() ? "" : "\n" + razon) + "\nHay cupos disponibles.");
        return datos;
    }
}
