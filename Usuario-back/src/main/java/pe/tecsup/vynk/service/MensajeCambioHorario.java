package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import pe.tecsup.vynk.model.AlertaHorario;

/** Contenido de la alerta de cambio de horario: viaja como «datos» para que la app muestre el horario anterior y el nuevo. */
public final class MensajeCambioHorario {
    /** La app reconoce por este valor que el aviso es un cambio de horario. */
    public static final String TIPO = "cambio_horario";

    private MensajeCambioHorario() {}

    public static Map<String, String> datos(AlertaHorario a) {
        String nombre = a.getNombreActividad() == null ? "" : a.getNombreActividad().strip().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) nombre = "tu actividad";
        if (nombre.length() > 80) nombre = nombre.substring(0, 79) + "…";
        String anterior = new Horario(a.getFechaAnterior(), a.getInicioAnterior(), a.getFinAnterior()).paraMostrar();
        String nuevo = new Horario(a.getFechaNueva(), a.getInicioNuevo(), a.getFinNuevo()).paraMostrar();
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("tipo", TIPO);
        datos.put("actividadId", a.getActividadId());
        datos.put("inscripcionId", a.getInscripcionId());
        datos.put("titulo", "Cambió el horario de " + nombre);
        datos.put("horarioAnterior", anterior);
        datos.put("horarioNuevo", nuevo);
        datos.put("cuerpo", "Antes: " + anterior + "\nAhora: " + nuevo);
        return datos;
    }
}
