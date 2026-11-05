package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import pe.tecsup.vynk.model.ConversacionAsistente;

/** Resuelve referencias breves sin permitir que el historial modifique el catálogo oficial. */
public final class ContextoConversacionService {
    private ContextoConversacionService() {}
    private static final Pattern REFERENCIA = Pattern.compile("\\b(ese|esa|eso|alli|ahi|anterior|mismo|misma|sus|su|y)\\b");
    private static final Pattern LUGAR = Pattern.compile("\\b(donde|ubicacion|lugar|sede|aula|salon|pabellon|direccion)\\b");
    private static final Pattern REQUISITO = Pattern.compile("\\b(requisitos?|materiales|llevar|necesito|prerrequisitos?)\\b");
    private static final Pattern HORA = Pattern.compile("\\b(hora|horario|cuando|empieza|comienza|termina|dura)\\b");

    public record Resolucion(String pregunta, String aclaracion) {}

    public static Resolucion resolver(String pregunta, List<ConversacionAsistente.Mensaje> mensajes) {
        String q = normalizar(pregunta);
        if (!REFERENCIA.matcher(q).find() || (!LUGAR.matcher(q).find() && !REQUISITO.matcher(q).find() && !HORA.matcher(q).find()))
            return new Resolucion(pregunta, "");
        if (mensajes == null || mensajes.isEmpty()) return new Resolucion(pregunta, "");
        for (int i = mensajes.size() - 1; i >= 0; i--) {
            var m = mensajes.get(i);
            if (!"asistente".equals(m.rol())) continue;
            List<Map<String, Object>> referencias = !m.actividades().isEmpty() ? m.actividades() : m.requisitos();
            // No reutilizar una actividad antigua si la última respuesta fue otra conversación.
            if (referencias.isEmpty()) return new Resolucion(pregunta, "");
            if (referencias.size() > 1) {
                return new Resolucion(pregunta, "En el mensaje anterior mencioné varias actividades. ¿De cuál deseas conocer ese dato? Escribe su nombre para consultar la información publicada.");
            }
            String nombre = String.valueOf(referencias.get(0).getOrDefault("nombre", "")).trim();
            if (nombre.isBlank()) return new Resolucion(pregunta, "");
            // Solo reutilizamos el nombre de un elemento ya validado por el catálogo.
            if (REQUISITO.matcher(q).find()) return new Resolucion("¿Qué requisitos tiene " + nombre + "?", "");
            if (LUGAR.matcher(q).find()) return new Resolucion("¿Dónde es " + nombre + "?", "");
            return new Resolucion("¿A qué hora empieza " + nombre + "?", "");
        }
        return new Resolucion(pregunta, "");
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
