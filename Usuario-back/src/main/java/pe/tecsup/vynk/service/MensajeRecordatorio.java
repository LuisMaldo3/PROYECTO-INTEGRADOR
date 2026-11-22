package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import pe.tecsup.vynk.model.Inscripcion;

/**
 * Contenido del aviso. Viaja como «datos» (no como notificación del sistema) para que la app
 * decida cómo mostrarlo y a qué actividad abrir cuando el estudiante lo toque.
 */
public final class MensajeRecordatorio {
    /** La app reconoce por este valor que el aviso es un recordatorio de actividad. */
    public static final String TIPO = "recordatorio_actividad";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private MensajeRecordatorio() {}

    /** @param lugar nombre de la ubicación (la inscripción solo guarda su identificador). */
    public static Map<String, String> datos(Inscripcion i, String lugar, Instant inicio, Instant ahora) {
        long minutos = Math.max(1, (Duration.between(ahora, inicio).toSeconds() + 59) / 60);
        String nombre = recortar(i.getNombreActividad(), 80, "Tu actividad");
        lugar = NombresUbicacion.POR_CONFIRMAR.equals(lugar) ? "" : recortar(lugar, 60, "");
        String hora = HORA.format(inicio.atZone(ProgramadorRecordatorios.LIMA));
        String cuerpo = nombre + " empieza en " + minutos + " min (" + hora + ")"
                + (lugar.isEmpty() ? "" : " · " + lugar);
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("tipo", TIPO);
        datos.put("actividadId", i.getActividadId());
        datos.put("inscripcionId", i.getId());
        datos.put("titulo", "Tu actividad empieza pronto");
        datos.put("cuerpo", cuerpo);
        datos.put("inicio", inicio.toString());
        return datos;
    }

    private static String recortar(String texto, int maximo, String porDefecto) {
        String limpio = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) return porDefecto;
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 1) + "…";
    }
}
