package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** US-59: la pertenencia a un equipo no sustituye la inscripción individual de cada alumno. */
@Service
public class OrganizadorEquiposVerificacionService {
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public OrganizadorEquiposVerificacionService(InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    public Map<String, Object> consultar(String actividadId, Integer limitePorEquipo) {
        List<Inscripcion> activas = inscripciones.findByActividadId(actividadId).stream()
                .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).toList();
        Map<String, Inscripcion> porEstudiante = new HashMap<>();
        for (Inscripcion inscripcion : activas) {
            if (inscripcion.getEstudianteId() != null && !inscripcion.getEstudianteId().isBlank()) {
                porEstudiante.put(inscripcion.getEstudianteId(), inscripcion);
            }
        }
        List<Map<String, Object>> equipos = new ArrayList<>();
        long completos = 0;
        for (Inscripcion capitan : activas) {
            if (!OrganizadorCuposService.esCapitan(capitan)) continue;
            List<String> integrantes = capitan.getIntegrantes();
            Set<String> unicos = new HashSet<>();
            List<Map<String, Object>> participantes = new ArrayList<>();
            int inscritosVerificados = 0;
            boolean integrantesValidos = true;
            for (String estudianteId : integrantes) {
                if (estudianteId == null || estudianteId.isBlank() || !unicos.add(estudianteId)) {
                    integrantesValidos = false;
                    continue;
                }
                Inscripcion personal = porEstudiante.get(estudianteId);
                boolean esCapitan = estudianteId.equals(capitan.getEstudianteId());
                boolean confirmada = personal != null && capitan.getId() != null
                        && capitan.getId().equals(personal.getEquipoId())
                        && (esCapitan ? OrganizadorCuposService.esCapitan(personal)
                                : "integrante".equals(personal.getRolEquipo()));
                if (confirmada) inscritosVerificados++;
                else integrantesValidos = false;
                String nombre = usuarios.findById(estudianteId).map(Usuario::getNombre)
                        .filter(n -> n != null && !n.isBlank())
                        .orElse(personal != null && personal.getNombreEstudiante() != null
                                ? personal.getNombreEstudiante() : "Estudiante sin datos");
                participantes.add(Map.of("estudianteId", estudianteId, "nombre", nombre,
                        "inscritoIndividualmente", confirmada, "rol", esCapitan ? "Capitán" : "Integrante"));
            }
            if (!unicos.contains(capitan.getEstudianteId())) integrantesValidos = false;
            boolean tamanoValido = integrantes.size() >= 2
                    && (limitePorEquipo == null || integrantes.size() <= limitePorEquipo);
            boolean completo = integrantesValidos && tamanoValido;
            if (completo) completos++;
            Map<String, Object> equipo = new LinkedHashMap<>();
            equipo.put("equipoId", capitan.getId());
            equipo.put("nombre", capitan.getNombreEquipo() == null || capitan.getNombreEquipo().isBlank()
                    ? "Equipo sin nombre" : capitan.getNombreEquipo());
            equipo.put("totalIntegrantes", integrantes.size());
            equipo.put("inscripcionesConfirmadas", inscritosVerificados);
            equipo.put("inscripcionesPendientes", integrantes.size() - inscritosVerificados);
            equipo.put("faltanIntegrantesMinimos", Math.max(0, 2 - integrantes.size()));
            equipo.put("superaLimite", limitePorEquipo != null && integrantes.size() > limitePorEquipo);
            equipo.put("completo", completo);
            equipo.put("integrantes", participantes);
            equipos.add(equipo);
        }
        equipos.sort(Comparator.comparing(e -> String.valueOf(e.get("nombre"))));
        return Map.of("totalEquipos", equipos.size(), "equiposCompletos", completos,
                "equiposPendientes", equipos.size() - completos, "equipos", equipos,
                "limiteIntegrantesEquipo", limitePorEquipo == null ? 0 : limitePorEquipo);
    }
}
