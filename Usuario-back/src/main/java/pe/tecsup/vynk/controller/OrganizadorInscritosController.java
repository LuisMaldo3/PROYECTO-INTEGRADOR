package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.model.Asistencia;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.AsistenciaRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;
import pe.tecsup.vynk.service.OrganizadorAccessService;

@RestController
@RequestMapping("/api/organizador/actividades/{actividadId}/inscritos")
public class OrganizadorInscritosController {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final AsistenciaRepository asistencia;
    public OrganizadorInscritosController(OrganizadorAccessService acceso, InscripcionRepository inscripciones,
                                         UsuarioRepository usuarios, AsistenciaRepository asistencia) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.asistencia = asistencia;
    }
    @GetMapping
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId) {
        Usuario responsable = acceso.exigirOrganizador(jwt);
        var actividad = acceso.exigirActividadPropia(responsable, actividadId);
        Map<String, Asistencia> marcas = new HashMap<>();
        for (Asistencia a : asistencia.findByActividadId(actividadId)) marcas.put(a.getEstudianteId(), a);
        var inscritos = inscripciones.findByActividadId(actividadId);
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (var i : inscritos) {
            if (!"inscrita".equalsIgnoreCase(i.getEstado())) continue;
            var alumno = usuarios.findById(i.getEstudianteId());
            Map<String, Object> dato = new LinkedHashMap<>();
            dato.put("estudianteId", i.getEstudianteId());
            dato.put("inscripcionId", i.getId());
            dato.put("nombre", alumno.map(Usuario::getNombre).filter(n -> !n.isBlank()).orElse("Estudiante sin datos"));
            dato.put("correo", alumno.map(Usuario::getCorreo).orElse(""));
            Asistencia marca = marcas.get(i.getEstudianteId());
            dato.put("asistenciaRegistrada", marca != null);
            dato.put("presente", marca == null ? null : marca.isPresente());
            resultado.add(dato);
        }
        resultado.sort(Comparator.comparing(a -> String.valueOf(a.get("nombre")).toLowerCase()));
        return Map.of("actividad", acceso.resumen(actividad), "total", resultado.size(), "inscritos", resultado);
    }
}
