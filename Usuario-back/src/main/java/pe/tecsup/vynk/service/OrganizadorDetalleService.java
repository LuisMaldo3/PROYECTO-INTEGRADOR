package pe.tecsup.vynk.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Detalle exclusivo de actividad propia; nombre de responsables obtenido del servidor. */
@Service
public class OrganizadorDetalleService {
    private static final List<String> AREAS = List.of("Gestión y Producción", "Tecnología Digital",
            "Minería y Procesos Químicos", "Electricidad y Electrónica", "Mecánica");
    private final OrganizadorAccessService acceso;
    private final OrganizadorCuposService cupos;
    private final OrganizadorHorarioService horario;
    private final UsuarioRepository usuarios;

    public OrganizadorDetalleService(OrganizadorAccessService acceso, OrganizadorCuposService cupos,
            OrganizadorHorarioService horario, UsuarioRepository usuarios) {
        this.acceso = acceso; this.cupos = cupos; this.horario = horario; this.usuarios = usuarios;
    }

    public Map<String, Object> detalle(Jwt jwt, String actividadId) {
        Usuario organizador = acceso.exigirOrganizador(jwt);
        Document actividad = acceso.exigirActividadPropia(organizador, actividadId);
        Map<String, Object> respuesta = new LinkedHashMap<>(acceso.resumen(actividad));
        respuesta.put("organizadorPrincipal", organizador.getNombre());
        List<Map<String, Object>> coordinadores = new ArrayList<>();
        Object raw = actividad.get("encargadosPorArea");
        Map<?, ?> porArea = raw instanceof Map<?, ?> m ? m : Map.of();
        for (String area : AREAS) {
            Object valor = porArea.get(area);
            String id = valor == null ? "" : String.valueOf(valor).trim();
            String nombre = id.isBlank() ? "" : usuarios.findById(id).map(Usuario::getNombre).orElse("");
            coordinadores.add(Map.of("area", area, "nombre", nombre == null ? "" : nombre,
                    "asignado", nombre != null && !nombre.isBlank()));
        }
        respuesta.put("coordinadores", coordinadores);
        return horario.enriquecer(cupos.enriquecer(respuesta));
    }
}
