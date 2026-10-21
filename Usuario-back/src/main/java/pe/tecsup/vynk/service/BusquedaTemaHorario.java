package pe.tecsup.vynk.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Aplica tema y horario SOBRE EL MISMO EVENTO publicado; nunca mezcla coincidencias. */
public final class BusquedaTemaHorario {
    private BusquedaTemaHorario() {}

    public static List<Map<String, Object>> buscar(List<Map<String, Object>> catalogo,
                                                     ConsultaTemaHorario consulta, Clock reloj) {
        if (catalogo == null || consulta == null || consulta.tema() == null) return List.of();
        LocalDate hoy = LocalDate.now(reloj);
        return catalogo.stream()
                .filter(a -> "programada".equalsIgnoreCase(texto(a, "estado")))
                .filter(a -> {
                    String fecha = texto(a, "fecha");
                    String iso = fecha.length() >= 10 ? fecha.substring(0, 10) : "";
                    try {
                        LocalDate dia = LocalDate.parse(iso);
                        return consulta.fecha() == null ? !dia.isBefore(hoy) : consulta.fecha().equals(dia);
                    } catch (Exception ex) { return false; }
                })
                .filter(a -> {
                    Object libres = a.get("cuposDisponibles");
                    if (libres == null) return true; // No afirmar cupos cuando son desconocidos.
                    try { return Integer.parseInt(String.valueOf(libres)) > 0; }
                    catch (NumberFormatException ex) { return false; }
                })
                .filter(a -> {
                    // Si no indicó fecha, ofrecer solo eventos que aún no terminaron.
                    if (consulta.fecha() != null) return true;
                    try {
                        LocalDate dia = LocalDate.parse(texto(a, "fecha").substring(0, 10));
                        return !dia.equals(hoy) || LocalTime.parse(texto(a, "horaFin")).isAfter(LocalTime.now(reloj));
                    } catch (Exception ex) { return false; }
                })
                .filter(a -> esDelTema(a, consulta.tema()))
                .filter(a -> coincideHora(a, consulta))
                .sorted(Comparator.comparing((Map<String, Object> a) -> texto(a, "fecha"))
                        .thenComparing(a -> texto(a, "horaInicio")))
                .limit(45).toList();
    }

    static boolean esDelTema(Map<String, Object> a, String tema) {
        String datos = ConsultaTemaHorario.normalizar(texto(a, "nombre") + " " + texto(a, "descripcion")
                + " " + texto(a, "categoriaId"));
        return switch (tema) {
            case "deportes" -> contiene(datos, "deport", "futbol", "voley", "voleibol", "basquet",
                    "basket", "atletismo", "gymkana", "torneo deportivo", "maraton");
            case "tecnologia" -> contiene(datos, "tecnolog", "programacion", "robotica", "software",
                    "hackathon", "informatica", "desarrollo web");
            case "arte" -> contiene(datos, "arte", "artist", "pintura", "dibujo", "fotografia",
                    "teatro", "danza", "escultura");
            case "musica" -> contiene(datos, "musica", "concierto", "canto", "karaoke");
            case "danza" -> contiene(datos, "danza", "baile", "coreografia");
            case "teatro" -> contiene(datos, "teatro", "escenic");
            case "emprendimiento" -> contiene(datos, "emprend", "negocio", "startup");
            case "gastronomia" -> contiene(datos, "gastronom", "cocina", "comida");
            case "videojuegos" -> contiene(datos, "videojuego", "esports", "e-sports", "gaming");
            case "ia" -> contiene(datos, "inteligencia artificial", "machine learning", "ia aplicada");
            case "fotografia" -> contiene(datos, "fotografia", "fotografic");
            default -> datos.contains(tema);
        };
    }

    private static boolean contiene(String datos, String... pistas) {
        for (String pista : pistas) if (datos.contains(pista)) return true;
        return false;
    }

    static boolean coincideHora(Map<String, Object> a, ConsultaTemaHorario consulta) {
        if (consulta.hora() == null) return true;
        try {
            LocalTime inicio = LocalTime.parse(texto(a, "horaInicio"));
            LocalTime fin = LocalTime.parse(texto(a, "horaFin"));
            if (!fin.isAfter(inicio)) return false;
            return switch (consulta.modo()) {
                case DESPUES -> !inicio.isBefore(consulta.hora());
                case ANTES -> inicio.isBefore(consulta.hora());
                case EN, CUALQUIERA -> !consulta.hora().isBefore(inicio) && consulta.hora().isBefore(fin);
            };
        } catch (Exception e) { return false; }
    }

    private static String texto(Map<String, Object> item, String clave) {
        Object valor = item.get(clave);
        return valor == null ? "" : String.valueOf(valor).trim();
    }
}
