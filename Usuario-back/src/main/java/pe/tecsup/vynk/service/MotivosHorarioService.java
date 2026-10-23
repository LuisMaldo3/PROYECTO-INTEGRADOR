package pe.tecsup.vynk.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Motivos reproducibles basados únicamente en intereses y franjas comprobadas. */
public final class MotivosHorarioService {
    private MotivosHorarioService() {}
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public static String explicacion(Map<String, Object> actividad, List<String> intereses,
                                     LocalDate fecha, LocalTime inicioLibre, LocalTime finLibre) {
        String texto = (String.valueOf(actividad.getOrDefault("nombre", "")) + " "
                + String.valueOf(actividad.getOrDefault("descripcion", "")) + " "
                + String.valueOf(actividad.getOrDefault("categoriaId", ""))).toLowerCase(Locale.ROOT);
        String interes = intereses.stream().filter(i -> i != null && !i.isBlank()
                && texto.contains(i.toLowerCase(Locale.ROOT))).findFirst().orElse("");
        String afinidad = interes.isBlank() ? "Es una actividad publicada disponible"
                : "Coincide con tu interés en " + interes.strip();
        return afinidad + " y cabe en tu horario libre del " + fecha
                + " entre " + inicioLibre.format(HORA) + " y " + finLibre.format(HORA) + ".";
    }

    public static String detalle(LocalTime inicioLibre, LocalTime finLibre) {
        return "Compatible con el espacio libre de " + inicioLibre.format(HORA) + " a " + finLibre.format(HORA)
                + ", según el horario que confirmaste y tus inscripciones actuales.";
    }
}
