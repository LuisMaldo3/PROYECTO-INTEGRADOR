package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;


/** Detalles de actividades propias. La autorización siempre se resuelve desde el JWT. */
@Service
public class OrganizadorDetalleService {
    private final OrganizadorAccessService acceso;
    private final OrganizadorCuposService cupos;
    private final OrganizadorHorarioService horario;

    public OrganizadorDetalleService(OrganizadorAccessService acceso, OrganizadorCuposService cupos, OrganizadorHorarioService horario) {
        this.acceso = acceso;
        this.cupos = cupos;
        this.horario = horario;
    }

    public Map<String, Object> detalle(Jwt jwt, String actividadId) {
        var organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        Map<String, Object> respuesta = new LinkedHashMap<>(acceso.resumen(actividad));
        return horario.enriquecer(cupos.enriquecer(respuesta));
    }
}
