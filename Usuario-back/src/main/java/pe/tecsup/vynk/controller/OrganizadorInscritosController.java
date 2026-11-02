package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.text.Normalizer;
import java.util.List;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.service.OrganizadorCuposService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/inscritos")
public class OrganizadorInscritosController {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    public OrganizadorInscritosController(OrganizadorAccessService acceso, InscripcionRepository inscripciones,
                                         UsuarioRepository usuarios) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }
    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                      @RequestParam(name = "buscar", defaultValue = "") String buscar) {
        Usuario responsable = acceso.exigirOrganizador(jwt);
        var actividad = acceso.exigirActividadPropia(responsable, actividadId);
        if (buscar.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La búsqueda supera 100 caracteres");
        String filtro = normalizar(buscar.trim());
        var inscritos = inscripciones.findByActividadId(actividadId);
        List<Map<String, Object>> resultado = new ArrayList<>();
        Map<String, Inscripcion> porAlumno = new HashMap<>();
        for (Inscripcion i : inscritos) if ("inscrita".equalsIgnoreCase(i.getEstado()))
            porAlumno.put(i.getEstudianteId(), i);
        for (var i : inscritos) {
            if (!"inscrita".equalsIgnoreCase(i.getEstado())) continue;
            var alumno = usuarios.findById(i.getEstudianteId());
            Map<String, Object> dato = new LinkedHashMap<>();
            dato.put("estudianteId", i.getEstudianteId());
            dato.put("inscripcionId", i.getId());
            dato.put("nombre", alumno.map(Usuario::getNombre).filter(n -> !n.isBlank()).orElse("Estudiante sin datos"));
            dato.put("correo", alumno.map(Usuario::getCorreo).orElse(""));
            dato.put("nombreEquipo", i.getNombreEquipo());
            dato.put("areaTecsup", i.isAreaVerificada() ? i.getAreaTecsup() : null);
            // Solo el organizador responsable ve a los integrantes: nunca se envían estos datos al catálogo público.
            List<Map<String, String>> miembros = new ArrayList<>();
            for (String idMiembro : i.getIntegrantes()) {
                var miembro = usuarios.findById(idMiembro);
                if (miembro.isPresent()) miembros.add(Map.of("nombre", miembro.get().getNombre() == null ? "" : miembro.get().getNombre(),
                        "correo", miembro.get().getCorreo() == null ? "" : miembro.get().getCorreo()));
            }
            dato.put("integrantes", miembros);
            dato.put("modalidad", i.getNombreEquipo() == null ? "individual" : "equipos");
            dato.put("rolEquipo", i.getRolEquipo());
            dato.put("equipoId", i.getEquipoId());
            dato.put("equipoHabilitado", OrganizadorCuposService.esCapitan(i)
                    && OrganizadorCuposService.equipoConfirmado(i, porAlumno));
            dato.put("integrantesConfirmados", OrganizadorCuposService.esCapitan(i)
                    ? i.getIntegrantes().stream().filter(id -> {
                        Inscripcion miembro = porAlumno.get(id);
                        return miembro != null && i.getId().equals(miembro.getEquipoId());
                    }).count() : 0L);
            resultado.add(dato);
        }
        resultado.sort(Comparator.comparing(a -> normalizar(String.valueOf(a.get("nombre")))));
        int totalInscritos = resultado.size();
        if (!filtro.isBlank()) {
            resultado.removeIf(a -> !normalizar(String.valueOf(a.get("nombre"))).contains(filtro)
                    && !normalizar(String.valueOf(a.get("correo"))).contains(filtro)
                    && !normalizar(String.valueOf(a.get("nombreEquipo"))).contains(filtro)
                    && !normalizar(String.valueOf(a.get("areaTecsup"))).contains(filtro));
        }
        long equiposRegistrados = inscritos.stream().filter(i -> "inscrita".equals(i.getEstado()))
                .filter(OrganizadorCuposService::esCapitan).count();
        long equiposConfirmados = inscritos.stream().filter(i -> "inscrita".equals(i.getEstado()))
                .filter(OrganizadorCuposService::esCapitan)
                .filter(i -> OrganizadorCuposService.equipoConfirmado(i, porAlumno)).count();
        return Map.of("actividad", acceso.resumen(actividad), "total", resultado.size(),
                "totalInscritos", totalInscritos, "inscritos", resultado,
                "equiposRegistrados", equiposRegistrados, "equiposConfirmados", equiposConfirmados);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
