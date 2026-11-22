package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import pe.tecsup.vynk.model.AlertaUbicacion;

/** Contenido de la alerta de cambio de lugar: viaja como «datos» para que la app muestre la ubicación anterior y la nueva. */
public final class MensajeCambioUbicacion {
    /** La app reconoce por este valor que el aviso es un cambio de ubicación. */
    public static final String TIPO = "cambio_ubicacion";

    private MensajeCambioUbicacion() {}

    public static Map<String, String> datos(AlertaUbicacion a) {
        String nombre = recortar(a.getNombreActividad(), 80, "tu actividad");
        String anterior = recortar(a.getUbicacionAnterior(), 60, NombresUbicacion.POR_CONFIRMAR);
        String nueva = recortar(a.getUbicacionNueva(), 60, NombresUbicacion.POR_CONFIRMAR);
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("tipo", TIPO);
        datos.put("actividadId", a.getActividadId());
        datos.put("inscripcionId", a.getInscripcionId());
        datos.put("titulo", "Cambió el lugar de " + nombre);
        datos.put("ubicacionAnterior", anterior);
        datos.put("ubicacionNueva", nueva);
        datos.put("cuerpo", "Antes: " + anterior + "\nAhora: " + nueva);
        return datos;
    }

    private static String recortar(String texto, int maximo, String porDefecto) {
        String limpio = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) return porDefecto;
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 1) + "…";
    }
}
