package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Jordi: interpretación verificable de expresiones temporales en español de Perú. */
public record ConsultaMomento(LocalDate fecha, LocalTime hora, LocalTime hasta, boolean aplicable,
                              String aclaracion) {
    private static final Pattern ISO = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");
    private static final Pattern LATINA = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})/(20\\d{2})\\b");
    private static final Pattern RELOJ = Pattern.compile("\\b(?:a\\s+las?|desde\\s+las?|sobre\\s+las?|a\\s+partir\\s+de\\s+las?)\\s*(\\d{1,2})(?:[:.](\\d{2}))?\\s*(am|pm|a\\.?\\s*m\\.?|p\\.?\\s*m\\.?|de\\s+la\\s+manana|de\\s+la\\s+tarde|de\\s+la\\s+noche|de\\s+la\\s+madrugada)?\\b");
    private static final Pattern HORA24 = Pattern.compile("(?<![\\d-])\\b(\\d{1,2}):(\\d{2})\\s*(am|pm|a\\.?\\s*m\\.?|p\\.?\\s*m\\.?)?\\b");
    private static final String[] DIAS = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"};

    public static ConsultaMomento interpretar(String pregunta, Clock reloj) {
        String q = normalizar(pregunta);
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fecha = null;
        Matcher iso = ISO.matcher(q);
        Matcher latina = LATINA.matcher(q);
        try {
            if (iso.find()) fecha = LocalDate.parse(iso.group(1));
            else if (latina.find()) fecha = LocalDate.of(Integer.parseInt(latina.group(3)), Integer.parseInt(latina.group(2)), Integer.parseInt(latina.group(1)));
        } catch (RuntimeException ex) {
            return new ConsultaMomento(null, null, null, true, "La fecha no es válida. Escríbela como AAAA-MM-DD.");
        }
        if (fecha == null) {
            if (q.matches(".*\\bpasado manana\\b.*")) fecha = hoy.plusDays(2);
            else if (q.matches(".*\\bmanana\\b.*")) fecha = hoy.plusDays(1);
            else if (q.matches(".*\\bhoy\\b.*")) fecha = hoy;
            else {
                for (int i = 0; i < DIAS.length; i++) {
                    if (q.matches(".*\\b" + DIAS[i] + "\\b.*")) {
                        DayOfWeek dia = DayOfWeek.of(i + 1);
                        int diferencia = (dia.getValue() - hoy.getDayOfWeek().getValue() + 7) % 7;
                        if (q.contains("proximo " + DIAS[i]) && diferencia == 0) diferencia = 7;
                        fecha = hoy.plusDays(diferencia);
                        break;
                    }
                }
            }
        }
        LocalTime hora = null;
        LocalTime hasta = null;
        if (q.matches(".*\\b(ahora|en este momento)\\b.*")) {
            if (fecha == null) fecha = hoy;
            hora = LocalTime.now(reloj).withSecond(0).withNano(0);
            return new ConsultaMomento(fecha, hora, null, true, "");
        }
        Matcher m = RELOJ.matcher(q);
        if (!m.find()) m = HORA24.matcher(q);
        if (m.find(0)) {
            int h = Integer.parseInt(m.group(1));
            int minutos = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            String periodo = m.group(3) == null ? "" : m.group(3).replace(".", "").replace(" ", "");
            if (h > 23 || minutos > 59 || (periodo.contains("m") && (h < 1 || h > 12))) {
                return new ConsultaMomento(fecha, null, null, true, "La hora indicada no es válida. Usa, por ejemplo, 15:00 o 3 p. m.");
            }
            if (periodo.equals("pm") || periodo.contains("tarde") || periodo.contains("noche")) h = h % 12 + 12;
            else if (periodo.equals("am") || periodo.contains("manana") || periodo.contains("madrugada")) h = h % 12;
            else if (h <= 12 && (m.group(2) == null || !m.group(1).startsWith("0"))) {
                return new ConsultaMomento(fecha, null, null, true, "¿Te refieres a las " + h + " a. m. o " + h + " p. m.? Indica la hora para buscar con precisión.");
            }
            hora = LocalTime.of(h, minutos);
        } else if (q.contains("por la tarde") || q.contains("en la tarde")) { hora = LocalTime.NOON; hasta = LocalTime.of(18, 0); }
        else if (q.contains("por la manana") || q.contains("en la manana")) { hora = LocalTime.of(6, 0); hasta = LocalTime.NOON; }
        else if (q.contains("por la noche") || q.contains("en la noche")) { hora = LocalTime.of(18, 0); hasta = LocalTime.MAX; }
        boolean aplicable = fecha != null || hora != null || hasta != null || RELOJ.matcher(q).find();
        if (hora != null && fecha == null) fecha = hoy;
        return new ConsultaMomento(fecha, hora, hasta, aplicable, "");
    }

    static String normalizar(String texto) {
        return Normalizer.normalize((texto == null ? "" : texto).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim();
    }
}
