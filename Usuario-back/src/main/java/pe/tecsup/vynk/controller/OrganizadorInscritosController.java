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

    /** US-57: bitácora de sustituciones confirmadas; nunca devuelve datos de actividades ajenas. */
    @GetMapping("/historial")
    public Map<String, Object> historial(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Usuario responsable = acceso.exigirOrganizador(jwt);
        acceso.exigirActividadPropia(responsable, actividadId);
        List<Map<String, Object>> movimientos = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones.findByActividadId(actividadId)) {
            if (inscripcion.getHistorialReemplazos() == null) continue;
            for (Map<String, Object> cambio : inscripcion.getHistorialReemplazos()) {
                if (cambio == null) continue;
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("inscripcionId", inscripcion.getId());
                Object fecha = cambio.get("fecha");
                fila.put("fecha", fecha instanceof java.util.Date d ? d.toInstant().toString()
                        : fecha == null ? "" : String.valueOf(fecha));
                fila.put("estudianteRetirado", nombreDeHistorial(cambio, "nombreRetirado", "estudianteRetiradoId"));
                fila.put("estudianteNuevo", nombreDeHistorial(cambio, "nombreNuevo", "estudianteNuevoId"));
                fila.put("motivo", cambio.get("motivo") == null ? "" : String.valueOf(cambio.get("motivo")));
                fila.put("nombreEquipo", cambio.get("nombreEquipo") == null ? "" : String.valueOf(cambio.get("nombreEquipo")));
                movimientos.add(fila);
            }
        }
        movimientos.sort(Comparator.comparing((Map<String, Object> fila) -> String.valueOf(fila.get("fecha"))).reversed());
        int total = movimientos.size();
        // Un historial extenso no debe saturar el panel; se muestran los últimos 200 movimientos.
        return Map.of("total", total, "historial", movimientos.subList(0, Math.min(total, 200)));
    }

    private String nombreDeHistorial(Map<String, Object> cambio, String claveNombre, String claveId) {
        Object nombreOriginal = cambio.get(claveNombre);
        if (nombreOriginal instanceof String nombre && !nombre.isBlank()) return nombre;
        Object id = cambio.get(claveId);
        if (id == null) return "Estudiante (sin datos)";
        return usuarios.findById(String.valueOf(id)).map(Usuario::getNombre)
                .filter(n -> n != null && !n.isBlank()).orElse("Estudiante (cuenta no disponible)");
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
