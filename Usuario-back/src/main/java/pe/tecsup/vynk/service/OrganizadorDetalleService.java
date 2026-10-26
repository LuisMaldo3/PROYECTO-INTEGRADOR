package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.repository.InscripcionRepository;

/** Detalles de actividades propias. La autorización siempre se resuelve desde el JWT. */
@Service
public class OrganizadorDetalleService {
    private final OrganizadorAccessService acceso;
    private final InscripcionRepository inscripciones;
    private final OrganizadorHorarioService horario;

    public OrganizadorDetalleService(OrganizadorAccessService acceso, InscripcionRepository inscripciones, OrganizadorHorarioService horario) {
        this.acceso = acceso;
        this.inscripciones = inscripciones;
        this.horario = horario;
    }

    public Map<String, Object> detalle(Jwt jwt, String actividadId) {
        var organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        Map<String, Object> respuesta = new LinkedHashMap<>(acceso.resumen(actividad));
        long inscritos = contarInscritos(actividadId);
        respuesta.put("inscritosRegistrados", inscritos);
        return horario.enriquecer(respuesta);
    }

    public long contarInscritos(String actividadId) {
        return inscripciones.findByActividadId(actividadId).stream()
                .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).count();
    }
}
