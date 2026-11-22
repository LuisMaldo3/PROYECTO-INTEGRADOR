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

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-PE"));

    /**
     * El interés se busca como palabra y sin tildes («ia» no coincide con «industria») y se nombra como lo diría
     * el estudiante («inteligencia artificial»); la fecha va legible («el lunes 23 de noviembre»).
     */
    public static String explicacion(Map<String, Object> actividad, List<String> intereses,
                                     LocalDate fecha, LocalTime inicioLibre, LocalTime finLibre) {
        String interes = RecomendacionesAppService.interesCoincidente(actividad, intereses);
        String afinidad = interes == null ? "Es una actividad publicada disponible"
                : "Coincide con tu interés en " + interes.strip();
        return afinidad + " y cabe en tu horario libre del " + fecha.format(DIA)
                + " entre " + inicioLibre.format(HORA) + " y " + finLibre.format(HORA) + ".";
    }

    public static String detalle(LocalTime inicioLibre, LocalTime finLibre) {
        return "Compatible con el espacio libre de " + inicioLibre.format(HORA) + " a " + finLibre.format(HORA)
                + ", según el horario que confirmaste y tus inscripciones actuales.";
    }
}
