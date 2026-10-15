package pe.tecsup.vynk.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Asistencia;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.AsistenciaRepository;
import pe.tecsup.vynk.repository.InscripcionRepository;

@Service
public class AsistenciaService {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final AsistenciaRepository asistencias;
    public AsistenciaService(OrganizadorAccessService acceso, InscripcionRepository inscripciones,
                             AsistenciaRepository asistencias) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.asistencias = asistencias;
    }
    public record Marca(String estudianteId, Boolean presente) {}
    public record Solicitud(List<Marca> asistencias) {}

    public Map<String, Object> guardar(Jwt jwt, String actividadId, Solicitud solicitud) {
        var organizador = acceso.exigirOrganizador(jwt);
        var actividad = acceso.exigirActividadPropia(organizador, actividadId);
        if ("cancelada".equalsIgnoreCase(String.valueOf(actividad.get("estado")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se registra asistencia en una actividad cancelada");
        }
        if (solicitud == null || solicitud.asistencias() == null || solicitud.asistencias().isEmpty()
                || solicitud.asistencias().size() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envía entre 1 y 500 marcas de asistencia");
        }
        Set<String> validos = new HashSet<>();
        for (Inscripcion i : inscripciones.findByActividadId(actividadId)) {
            if ("inscrita".equalsIgnoreCase(i.getEstado())) validos.add(i.getEstudianteId());
        }
        Set<String> vistos = new HashSet<>();
        for (Marca marca : solicitud.asistencias()) {
            if (marca == null || marca.estudianteId() == null || !validos.contains(marca.estudianteId())
                    || marca.presente() == null || !vistos.add(marca.estudianteId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lista contiene estudiantes no inscritos, repetidos o valores inválidos");
            }
        }
        Map<String, Asistencia> previas = new HashMap<>();
        for (Asistencia anterior : asistencias.findByActividadId(actividadId)) {
            previas.put(anterior.getEstudianteId(), anterior);
        }
        Instant ahora = Instant.now();
        List<Asistencia> guardar = new ArrayList<>();
        for (Marca marca : solicitud.asistencias()) {
            Asistencia a = previas.getOrDefault(marca.estudianteId(), new Asistencia());
            a.setActividadId(actividadId);
            a.setEstudianteId(marca.estudianteId());
            a.setPresente(marca.presente());
            a.setActualizadoEn(ahora);
            a.setRegistradoPor(organizador.getId());
            guardar.add(a);
        }
        asistencias.saveAll(guardar);
        return Map.of("mensaje", "Asistencia guardada", "guardados", guardar.size(), "actualizadoEn", ahora.toString());
    }
}
