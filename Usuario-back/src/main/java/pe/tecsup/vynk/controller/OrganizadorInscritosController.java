package pe.tecsup.vynk.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.model.Asistencia;
import pe.tecsup.vynk.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    public Map<String, Object> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable String actividadId,
                                      @RequestParam(name = "buscar", defaultValue = "") String buscar) {
        Usuario responsable = acceso.exigirOrganizador(jwt);
        var actividad = acceso.exigirActividadPropia(responsable, actividadId);
        if (buscar.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La búsqueda supera 100 caracteres");
        String filtro = normalizar(buscar.trim());
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
        resultado.sort(Comparator.comparing(a -> normalizar(String.valueOf(a.get("nombre")))));
        int totalInscritos = resultado.size();
        if (!filtro.isBlank()) {
            resultado.removeIf(a -> !normalizar(String.valueOf(a.get("nombre"))).contains(filtro)
                    && !normalizar(String.valueOf(a.get("correo"))).contains(filtro));
        }
        return Map.of("actividad", acceso.resumen(actividad), "total", resultado.size(),
                "totalInscritos", totalInscritos, "inscritos", resultado);
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
