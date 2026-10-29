package pe.tecsup.vynk.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.repository.InscripcionRepository;

/** Cupos calculados desde inscripciones activas; no confía en contadores obsoletos. */
@Service
public class OrganizadorCuposService {
    private final InscripcionRepository inscripciones;

    public OrganizadorCuposService(InscripcionRepository inscripciones) {
        this.inscripciones = inscripciones;
    }

    public Map<String, Object> enriquecer(Map<String, Object> actividad) {
        Map<String, Object> resultado = new LinkedHashMap<>(actividad);
        String id = String.valueOf(actividad.get("id"));
        long ocupados = inscripciones.findByActividadId(id).stream()
                .filter(i -> "inscrita".equalsIgnoreCase(i.getEstado())).count();
        Integer capacidad = capacidad(actividad.get("maximoParticipantes"));
        resultado.put("inscritosRegistrados", ocupados);
        resultado.put("cuposOcupados", ocupados);
        resultado.put("cuposDisponibles", capacidad == null ? null : Math.max(0L, (long) capacidad - ocupados));
        resultado.put("capacidadConfirmada", capacidad != null);
        return resultado;
    }

    static Integer capacidad(Object valor) {
        if (valor instanceof Number numero) return Math.max(0, numero.intValue());
        if (valor instanceof String texto && texto.matches("\\d{1,8}")) {
            try { return Integer.parseInt(texto); } catch (NumberFormatException ignorada) { return null; }
        }
        return null;
    }
}
