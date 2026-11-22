package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import pe.tecsup.vynk.model.Inscripcion;

/**
 * US-77-T06: sincroniza la agenda individual con los equipos. Cada integrante tiene su propia inscripción y el equipo
 * vive en la inscripción del capitán (lista de integrantes). Aquí se decide, igual que en «mi-inscripcion»:
 * <ul>
 *   <li>una inscripción individual entra tal cual;</li>
 *   <li>una de equipo entra con el estado del equipo: cuántos integrantes confirmaron y si el equipo está
 *       confirmado (al menos dos y todos confirmados);</li>
 *   <li>la de un integrante cuyo equipo ya no está activo (el capitán lo canceló) no ocupa su agenda.</li>
 * </ul>
 */
public final class EquiposAgenda {
    private EquiposAgenda() {}

    /** Estado del equipo visto desde la inscripción de un estudiante. */
    public record Equipo(String id, String nombre, String rol, int integrantes, int confirmados) {
        public boolean confirmado() { return confirmados >= 2 && confirmados == integrantes; }
        public int pendientes() { return Math.max(0, integrantes - confirmados); }
    }

    /** Una inscripción de la agenda; {@code equipo} es null si es individual. */
    public record Entrada(Inscripcion inscripcion, Equipo equipo) {}

    /** Lo que entra a la agenda y las inscripciones de integrante que quedaron sin equipo activo. */
    public record Resultado(List<Entrada> entradas, List<Inscripcion> sinEquipo) {}

    static boolean activa(Inscripcion i) {
        return i != null && "inscrita".equalsIgnoreCase(i.getEstado());
    }

    static boolean deEquipo(Inscripcion i) {
        return i.getRolEquipo() != null && !i.getRolEquipo().isBlank();
    }

    /**
     * @param mias           inscripciones del estudiante (se ignoran las canceladas)
     * @param porId          busca una inscripción por su id (la del capitán es el equipo)
     * @param deEstudiante   inscripción de un estudiante en una actividad
     */
    public static Resultado sincronizar(List<Inscripcion> mias, Function<String, Optional<Inscripcion>> porId,
                                        BiFunction<String, String, Optional<Inscripcion>> deEstudiante) {
        List<Entrada> entradas = new ArrayList<>();
        List<Inscripcion> sinEquipo = new ArrayList<>();
        if (mias == null) return new Resultado(entradas, sinEquipo);
        for (Inscripcion i : mias) {
            if (!activa(i)) continue;
            if (!deEquipo(i)) { entradas.add(new Entrada(i, null)); continue; }
            boolean integrante = "integrante".equals(i.getRolEquipo());
            Optional<Inscripcion> equipo = integrante
                    ? (i.getEquipoId() == null ? Optional.empty() : porId.apply(i.getEquipoId()))
                    : Optional.of(i);
            if (equipo.isEmpty() || !activa(equipo.get())) { sinEquipo.add(i); continue; }
            Inscripcion e = equipo.get();
            List<String> miembros = new ArrayList<>(new LinkedHashSet<>(
                    e.getIntegrantes() == null ? List.of() : e.getIntegrantes()));
            int confirmados = 0;
            for (String miembro : miembros) {
                boolean confirmo = deEstudiante.apply(miembro, e.getActividadId())
                        .filter(x -> activa(x) && e.getId() != null && e.getId().equals(x.getEquipoId()))
                        .isPresent();
                if (confirmo) confirmados++;
            }
            entradas.add(new Entrada(i, new Equipo(e.getId(), e.getNombreEquipo(),
                    integrante ? "integrante" : "capitan", miembros.size(), confirmados)));
        }
        return new Resultado(entradas, sinEquipo);
    }
}
