package pe.tecsup.vynk.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Requisitos verificados del catálogo de actividades publicado por administración. */
public final class RequisitosActividadesService {
    private RequisitosActividadesService() {}

    private static final Pattern INTENCION = Pattern.compile(
            "\\b(requisit(?:o|os)|prerrequisit(?:o|os)|condiciones|materiales|que (?:debo|tengo que|hay que) llevar|que necesito|necesito para (?:participar|asistir|inscribirme)|para participar)\\b");
    private static final Set<String> GENERICAS = Set.of(
            "que", "cuales", "son", "los", "las", "del", "de", "para", "el", "la", "en", "un", "una", "se", "hay", "tiene", "tienen", "tengo", "debo", "llevar", "necesito", "necesitan", "materiales", "requisitos", "requisito", "prerrequisitos", "prerrequisito", "condiciones", "actividad", "actividades", "participar", "asistir", "inscribirme", "sobre", "quiero", "saber", "dime", "informacion", "evento", "eventos", "taller", "charla", "tecsup", "vynk", "hacer", "estar");

    public static boolean esConsultaDeRequisitos(String pregunta) {
        return INTENCION.matcher(normalizar(pregunta)).find();
    }

    public static AsistenteHorariosService.Respuesta responder(String pregunta, List<Map<String, Object>> publicadas) {
        if (publicadas == null || publicadas.isEmpty())
            return respuesta("Aún no hay actividades publicadas para consultar sus requisitos.", List.of());
        String consulta = normalizar(pregunta);
        List<String> pistas = Pattern.compile("[^a-z0-9]+")
                .splitAsStream(consulta).filter(p -> p.length() > 2 && !GENERICAS.contains(p))
                .distinct().toList();
        if (pistas.isEmpty()) return respuesta("¿De qué actividad quieres conocer los requisitos? Escribe su nombre, por ejemplo: «¿Qué requisitos tiene el taller de robótica?».", List.of());

        List<Map<String, Object>> coincidencias = publicadas.stream()
                .filter(a -> a.get("nombre") != null)
                .filter(a -> {
                    String nombre = normalizar(String.valueOf(a.get("nombre")));
                    // Coincidencia con palabras completas, evitando confundir "red" con "redes".
                    Set<String> palabrasNombre = new HashSet<>(List.of(nombre.split("[^a-z0-9]+")));
                    return pistas.stream().allMatch(palabrasNombre::contains);
                })
                .sorted(Comparator.comparingInt((Map<String, Object> a) -> String.valueOf(a.get("nombre")).length()))
                .limit(7).toList();
        if (coincidencias.isEmpty())
            return respuesta("No encontré una actividad publicada con ese nombre. Revisa el nombre en el catálogo y vuelve a preguntarme.", List.of());
        if (coincidencias.size() > 1) {
            String opciones = coincidencias.stream().map(a -> "• " + a.get("nombre")).reduce((a, b) -> a + "\n" + b).orElse("");
            return respuesta("Encontré varias actividades parecidas. Indica el nombre completo para consultar los requisitos correctos:\n" + opciones, List.of());
        }
        Map<String, Object> actividad = coincidencias.get(0);
        String nombre = String.valueOf(actividad.get("nombre"));
        List<String> items = elementos(actividad.get("requisitos"));
        boolean confirmados = !items.isEmpty();
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("actividadId", String.valueOf(actividad.getOrDefault("id", "")));
        detalle.put("nombre", nombre);
        detalle.put("items", items);
        detalle.put("confirmados", confirmados);
        String texto = confirmados
                ? "Requisitos publicados para «" + nombre + "»:\n" + String.join("\n", items.stream().map(i -> "• " + i).toList())
                : "Administración todavía no ha especificado los requisitos de «" + nombre + "». Esto no significa que la actividad no tenga requisitos. Consulta al organizador para confirmarlo.";
        return respuesta(texto, List.of(detalle));
    }

    static List<String> elementos(Object bruto) {
        if (bruto == null) return List.of();
        List<?> valores = bruto instanceof List<?> lista ? lista : List.of(bruto);
        List<String> salida = new ArrayList<>();
        for (Object valor : valores) {
            if (valor == null) continue;
            String texto;
            if (valor instanceof Map<?, ?> m) {
                Object dato = m.containsKey("descripcion") ? m.get("descripcion")
                        : m.containsKey("nombre") ? m.get("nombre") : m.get("requisito");
                texto = String.valueOf(dato);
            } else texto = String.valueOf(valor);
            for (String item : texto.split("[\\r\\n;]+")) {
                String limpio = item.replaceFirst("^\\s*[-•*]\\s*", "").trim();
                if (!limpio.isBlank() && !limpio.equalsIgnoreCase("null") && !salida.contains(limpio))
                    salida.add(limpio.length() > 300 ? limpio.substring(0, 300) : limpio);
                if (salida.size() >= 12) return List.copyOf(salida);
            }
        }
        return List.copyOf(salida);
    }

    private static AsistenteHorariosService.Respuesta respuesta(String texto, List<Map<String, Object>> requisitos) {
        return new AsistenteHorariosService.Respuesta(texto, "catalogo",
                "Requisitos obtenidos de actividades publicadas por administración de Tecsup.",
                List.of(), List.of(), "requisitos", requisitos);
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim();
    }
}
