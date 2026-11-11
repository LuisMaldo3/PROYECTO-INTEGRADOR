package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;

/** US-60/61: indicadores de inscripciones, NO asistencia al evento. Solo lectura. */
@Service
public class OrganizadorParticipacionService {
    private final OrganizadorAccessService acceso;
    private final OrganizadorCuposService cupos;
    private final InscripcionRepository inscripciones;

    public OrganizadorParticipacionService(OrganizadorAccessService acceso,
            OrganizadorCuposService cupos, InscripcionRepository inscripciones) {
        this.acceso = acceso;
        this.cupos = cupos;
        this.inscripciones = inscripciones;
    }

    /** Usa el mismo cálculo de cupos de US-58 para que ambos paneles coincidan. */
    public Map<String, Object> calcular(Document actividad) {
        Map<String, Object> cifras = cupos.enriquecer(acceso.resumen(actividad));
        String actividadId = String.valueOf(cifras.get("id"));
        List<Inscripcion> registros = inscripciones.findByActividadId(actividadId);
        long reemplazos = registros.stream().mapToLong(i -> i.getHistorialReemplazos() == null
                ? 0L : i.getHistorialReemplazos().size()).sum();
        boolean porEquipos = "equipos".equals(cifras.get("unidadCupo"));
        long ocupados = numero(cifras.get("cuposOcupados"));
        long estudiantes = numero(cifras.get("estudiantesInscritos"));
        long equiposRegistrados = porEquipos ? numero(cifras.get("equiposRegistrados")) : 0L;
        long completos = porEquipos ? numero(cifras.get("equiposConfirmados")) : 0L;
        long pendientes = porEquipos ? numero(cifras.get("equiposPendientes")) : 0L;
        Integer maximo = OrganizadorCuposService.capacidad(cifras.get("maximoParticipantes"));
        Long porcentaje = maximo != null && maximo > 0 ? Math.round(ocupados * 100.0 / maximo) : null;
        Long porcentajeEquiposCompletos = porEquipos && equiposRegistrados > 0
                ? Math.round(completos * 100.0 / equiposRegistrados) : null;
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("actividadId", actividadId);
        respuesta.put("nombreActividad", String.valueOf(cifras.getOrDefault("nombre", "Actividad")));
        respuesta.put("unidadCupo", porEquipos ? "equipos" : "estudiantes");
        respuesta.put("estudiantesInscritos", estudiantes);
        respuesta.put("equiposRegistrados", equiposRegistrados);
        respuesta.put("equiposConfirmados", completos);
        respuesta.put("equiposPendientes", pendientes);
        respuesta.put("reemplazosRegistrados", reemplazos);
        respuesta.put("cuposOcupados", ocupados);
        respuesta.put("cuposDisponibles", cifras.get("cuposDisponibles"));
        respuesta.put("maximoParticipantes", maximo);
        respuesta.put("porcentajeParticipacion", porcentaje);
        respuesta.put("porcentajeEquiposCompletos", porcentajeEquiposCompletos);
        respuesta.put("excedeLimite", maximo != null && ocupados > maximo);
        respuesta.put("nivel", porcentaje == null ? "sin_capacidad"
                : porcentaje >= 80 ? "alto" : porcentaje >= 50 ? "medio" : "bajo");
        respuesta.put("interpretacion", "Participación estimada por ocupación de inscripciones, no por asistencia presencial.");
        return respuesta;
    }

    private static long numero(Object valor) {
        return valor instanceof Number n ? Math.max(0L, n.longValue()) : 0L;
    }
}
