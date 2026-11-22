package pe.tecsup.vynk.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Fecha (yyyy-MM-dd), hora de inicio y hora de fin (HH:mm) de una actividad, en hora de Perú.
 * Un valor ausente o vacío se guarda como "", así null y "" (como los deja Django) se consideran iguales.
 */
public record Horario(String fecha, String inicio, String fin) {
    private static final String[] DIAS = {"lun", "mar", "mié", "jue", "vie", "sáb", "dom"};

    public Horario {
        fecha = limpio(fecha);
        inicio = limpio(inicio);
        fin = limpio(fin);
    }

    public static Horario de(Object fecha, Object inicio, Object fin) {
        return new Horario(texto(fecha), texto(inicio), texto(fin));
    }

    /** Fecha y hora de inicio válidas: sin ellas no se puede avisar ni reprogramar nada. */
    public boolean completo() {
        return comienzo() != null;
    }

    /** Momento exacto en que empieza, o null si la fecha o la hora no son válidas. */
    public Instant comienzo() {
        try {
            return LocalDateTime.of(LocalDate.parse(fecha), LocalTime.parse(inicio))
                    .atZone(ProgramadorRecordatorios.LIMA).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Texto para el aviso, por ejemplo «sáb 22/11 · 10:00–11:30». */
    public String paraMostrar() {
        String dia;
        try {
            LocalDate d = LocalDate.parse(fecha);
            dia = DIAS[d.getDayOfWeek().getValue() - 1] + " " + String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue());
        } catch (DateTimeParseException e) {
            dia = fecha.isEmpty() ? "Fecha por confirmar" : fecha;
        }
        String horas = inicio.isEmpty() ? "hora por confirmar" : (fin.isEmpty() ? inicio : inicio + "–" + fin);
        return dia + " · " + horas;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static String limpio(String valor) {
        String s = valor == null ? "" : valor.strip();
        return "null".equals(s) ? "" : s;
    }
}
