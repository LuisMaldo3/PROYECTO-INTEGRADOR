package pe.tecsup.vynk.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Horarios y ubicaciones provenientes del catálogo administrado, sin exponer IDs como lugares. */
@Service
public class OrganizadorHorarioService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final UbicacionesActividadesService ubicaciones;

    public OrganizadorHorarioService(UbicacionesActividadesService ubicaciones) {
        this.ubicaciones = ubicaciones;
    }

    public Map<String, Object> enriquecer(Map<String, Object> actividad) {
        Map<String, Object> respuesta = new LinkedHashMap<>(actividad);
        Map<String, Object> lugar = ubicaciones.resolver(List.of(respuesta)).get(0);
        respuesta.put("ubicacion", lugar.get("confirmada").equals(true) ? lugar.get("lugar") : "Ubicación por confirmar");
        respuesta.put("ubicacionDetalle", lugar);
        return respuesta;
    }

    /** Marca solo la próxima actividad vigente, en hora local de Perú. */
    public void marcarProxima(List<Map<String, Object>> items) {
        LocalDateTime ahora = LocalDateTime.now(LIMA);
        Map<String, Object> proxima = null;
        LocalDateTime masCercana = null;
        for (Map<String, Object> item : items) {
            item.put("proxima", false);
            if (!"programada".equalsIgnoreCase(String.valueOf(item.get("estado")))) continue;
            LocalDateTime horario = instante(item);
            if (horario != null && !horario.isBefore(ahora)
                    && (masCercana == null || horario.isBefore(masCercana))) {
                proxima = item;
                masCercana = horario;
            }
        }
        if (proxima != null) proxima.put("proxima", true);
    }

    static LocalDateTime instante(Map<String, Object> item) {
        try {
            String fecha = String.valueOf(item.get("fecha"));
            String hora = String.valueOf(item.get("horaInicio"));
            if (!hora.matches("\\d{2}:\\d{2}")) return null;
            return LocalDateTime.of(LocalDate.parse(fecha), LocalTime.parse(hora));
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }
}
