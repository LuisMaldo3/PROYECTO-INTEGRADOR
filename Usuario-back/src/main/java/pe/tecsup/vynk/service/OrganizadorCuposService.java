package pe.tecsup.vynk.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;

/** US-31/US-52: aforo real según modalidad. Los integrantes nunca consumen plazas de equipo. */
@Service
public class OrganizadorCuposService {
    private final InscripcionRepository inscripciones;

    public OrganizadorCuposService(InscripcionRepository inscripciones) { this.inscripciones = inscripciones; }

    public Map<String, Object> enriquecer(Map<String, Object> actividad) {
        Map<String, Object> resultado = new LinkedHashMap<>(actividad);
        String id = String.valueOf(actividad.get("id"));
        List<Inscripcion> confirmadas = inscripciones.findByActividadId(id).stream()
                .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).toList();
        boolean equipos = "competencia".equals(actividad.get("tipoActividad"))
                && "equipos".equals(actividad.get("modalidad"));
        long alumnos = confirmadas.stream().map(i ->
                i.getEstudianteId() != null && !i.getEstudianteId().isBlank()
                        ? (Object) i.getEstudianteId() : i).distinct().count();
        long registrados = confirmadas.stream().filter(OrganizadorCuposService::esCapitan).count();
        Map<String, Inscripcion> porAlumno = new HashMap<>();
        for (Inscripcion i : confirmadas) porAlumno.put(i.getEstudianteId(), i);
        long habilitados = confirmadas.stream().filter(OrganizadorCuposService::esCapitan)
                .filter(c -> equipoConfirmado(c, porAlumno)).count();
        long ocupados = equipos ? registrados : alumnos;
        Integer capacidad = capacidad(actividad.get("maximoParticipantes"));
        resultado.put("participantesTotales", alumnos);
        resultado.put("participantesCompetitivos", equipos ? habilitados : alumnos);
        resultado.put("inscritosRegistrados", alumnos);
        resultado.put("estudiantesInscritos", alumnos);
        resultado.put("equiposRegistrados", equipos ? registrados : 0L);
        resultado.put("equiposConfirmados", equipos ? habilitados : 0L);
        resultado.put("equiposPendientes", equipos ? registrados - habilitados : 0L);
        resultado.put("cuposOcupados", ocupados);
        resultado.put("unidadCupo", equipos ? "equipos" : "estudiantes");
        resultado.put("cuposDisponibles", capacidad == null ? null : Math.max(0L, (long) capacidad - ocupados));
        resultado.put("capacidadConfirmada", capacidad != null);
        return resultado;
    }

    public static boolean esCapitan(Inscripcion i) {
        return "capitan".equals(i.getRolEquipo()) || (i.getCodigoEquipoHash() != null
                && !i.getCodigoEquipoHash().isBlank());
    }

    public static boolean equipoConfirmado(Inscripcion capitan, Map<String, Inscripcion> porAlumno) {
        if (!esCapitan(capitan) || capitan.getId() == null || capitan.getIntegrantes().size() < 2
                || !capitan.getIntegrantes().contains(capitan.getEstudianteId())
                || capitan.getIntegrantes().stream().distinct().count() != capitan.getIntegrantes().size()) return false;
        return capitan.getIntegrantes().stream().allMatch(id -> {
            Inscripcion miembro = porAlumno.get(id);
            return miembro != null && "inscrita".equalsIgnoreCase(miembro.getEstado())
                    && capitan.getId().equals(miembro.getEquipoId())
                    && (id.equals(capitan.getEstudianteId()) ? esCapitan(miembro)
                            : "integrante".equals(miembro.getRolEquipo()));
        });
    }

    static Integer capacidad(Object valor) {
        if (valor instanceof Number numero) return Math.max(0, numero.intValue());
        if (valor instanceof String texto && texto.matches("\\d{1,8}")) {
            try { return Integer.parseInt(texto); } catch (NumberFormatException ignorada) { return null; }
        }
        return null;
    }
}
