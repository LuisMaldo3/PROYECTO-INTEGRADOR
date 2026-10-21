package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrae tema y restricciones de hora de preguntas cotidianas, sin depender de Gemini. */
public record ConsultaTemaHorario(String tema, LocalDate fecha, LocalTime hora,
                                  Modo modo, boolean tardeInferida, String aclaracion) {
    public enum Modo { CUALQUIERA, DESPUES, ANTES, EN }

    private static final Pattern HORA = Pattern.compile(
        "\\b(despues\\s+de|a\\s+partir\\s+de|desde|antes\\s+de|hasta|a\\s+las?)\\s+(?:las?\\s+)?" +
        "(\\d{1,2})(?:[:.](\\d{2}))?\\s*" +
        "(p\\.?\\s*m\\.?|a\\.?\\s*m\\.?|de\\s+la\\s+tarde|de\\s+la\\s+manana|de\\s+la\\s+noche)?(?=\\b|$)");
    private static final Map<String, String> TEMAS = Map.ofEntries(
        Map.entry("deport", "deportes"), Map.entry("futbol", "deportes"),
        Map.entry("voley", "deportes"), Map.entry("voleibol", "deportes"),
        Map.entry("basquet", "deportes"), Map.entry("basket", "deportes"),
        Map.entry("atletismo", "deportes"), Map.entry("gymkana", "deportes"),
        Map.entry("tecnolog", "tecnologia"), Map.entry("programacion", "tecnologia"),
        Map.entry("robotica", "tecnologia"), Map.entry("musica", "musica"),
        Map.entry("concierto", "musica"), Map.entry("arte", "arte"),
        Map.entry("teatro", "teatro"), Map.entry("danza", "danza"),
        Map.entry("baile", "danza"), Map.entry("emprend", "emprendimiento"),
        Map.entry("gastronom", "gastronomia"), Map.entry("cocina", "gastronomia"),
        Map.entry("videojuego", "videojuegos"), Map.entry("inteligencia artificial", "ia"),
        Map.entry("fotografia", "fotografia")
    );

    public static ConsultaTemaHorario interpretar(String pregunta, Clock reloj) {
        String texto = normalizar(pregunta);
        String tema = null;
        // Buscar coincidencias completas para evitar detectar 'arte' dentro de 'parte'.
        for (var entrada : TEMAS.entrySet()) {
            if (Pattern.compile("(?<![a-z])" + Pattern.quote(entrada.getKey()) + "[a-z]*\\b")
                    .matcher(texto).find()) {
                tema = entrada.getValue();
                break;
            }
        }
        if (tema == null) return new ConsultaTemaHorario(null, null, null, Modo.CUALQUIERA, false, "");
        ConsultaMomento temporal = ConsultaMomento.interpretar(pregunta, reloj);
        boolean indicoDia = Pattern.compile("\\b(hoy|manana|pasado manana|lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b|20\\d{2}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/20\\d{2}").matcher(texto).find();
        LocalDate fecha = indicoDia ? temporal.fecha() : null;
        Matcher m = HORA.matcher(texto);
        if (!m.find()) return new ConsultaTemaHorario(tema, fecha, temporal.hora(),
            temporal.hora() == null ? Modo.CUALQUIERA : Modo.EN, false, temporal.aclaracion());
        String relacion = m.group(1);
        Modo modo = relacion.startsWith("despues") || relacion.startsWith("a partir") || relacion.equals("desde")
                ? Modo.DESPUES : relacion.startsWith("antes") || relacion.equals("hasta") ? Modo.ANTES : Modo.EN;
        int horas = Integer.parseInt(m.group(2));
        int minutos = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        String periodo = m.group(4) == null ? "" : m.group(4).replace(".", "").replace(" ", "");
        if (horas > 23 || minutos > 59 || (!periodo.isBlank() && (horas < 1 || horas > 12)))
            return new ConsultaTemaHorario(tema, fecha, null, modo, false, "La hora no es válida. Usa 15:00 o 3 p. m.");
        boolean inferida = false;
        if (periodo.equals("pm") || periodo.contains("tarde") || periodo.contains("noche")) horas = horas % 12 + 12;
        else if (periodo.equals("am") || periodo.contains("manana")) horas = horas % 12;
        else if (horas <= 12 && m.group(3) == null) {
            // En 'después de las 3' el contexto habitual estudiantil es 15:00;
            // se declara la suposición. Para 'a las 3' se pide aclaración.
            if (modo == Modo.DESPUES && horas >= 1 && horas <= 7) {
                horas += 12; inferida = true;
            } else {
                return new ConsultaTemaHorario(tema, fecha, null, modo, false,
                    "¿Te refieres a las " + horas + " a. m. o p. m.? Escríbelo para buscar con precisión.");
            }
        }
        return new ConsultaTemaHorario(tema, fecha, LocalTime.of(horas, minutos), modo, inferida, "");
    }

    static String normalizar(String s) {
        return Normalizer.normalize((s == null ? "" : s).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
