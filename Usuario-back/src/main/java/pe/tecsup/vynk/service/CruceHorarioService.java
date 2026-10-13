package pe.tecsup.vynk.service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Inscripcion;

@Service
public class CruceHorarioService {
    public record Cruce(String id, String actividadAId, String actividadBId, String actividadA,
                        String actividadB, String fecha, String desde, String hasta, int minutos) {}

    public List<Cruce> detectar(List<Inscripcion> agenda) {
        List<Cruce> cruces = new ArrayList<>();
        for (int i = 0; i < agenda.size(); i++) {
            Inscripcion a = agenda.get(i);
            if (!vigente(a)) continue;
            for (int j = i + 1; j < agenda.size(); j++) {
                Inscripcion b = agenda.get(j);
                if (!vigente(b) || !a.getFecha().equals(b.getFecha())) continue;
                try {
                    LocalTime inicioA = LocalTime.parse(a.getHoraInicio());
                    LocalTime finA = LocalTime.parse(a.getHoraFin());
                    LocalTime inicioB = LocalTime.parse(b.getHoraInicio());
                    LocalTime finB = LocalTime.parse(b.getHoraFin());
                    if (!inicioA.isBefore(finA) || !inicioB.isBefore(finB)) continue;
                    LocalTime desde = inicioA.isAfter(inicioB) ? inicioA : inicioB;
                    LocalTime hasta = finA.isBefore(finB) ? finA : finB;
                    if (desde.isBefore(hasta)) {
                        int minutos = (int) java.time.Duration.between(desde, hasta).toMinutes();
                        cruces.add(new Cruce(String.valueOf(i) + "-" + j, a.getActividadId(), b.getActividadId(),
                            a.getNombreActividad(), b.getNombreActividad(), a.getFecha(), desde.toString(), hasta.toString(), minutos));
                    }
                } catch (DateTimeParseException | NullPointerException ignored) {
                    // Horarios incompletos no generan falsos positivos.
                }
            }
        }
        return cruces;
    }
    private boolean vigente(Inscripcion i) {
        return i != null && i.getFecha() != null && !i.getFecha().isBlank()
            && !"cancelada".equalsIgnoreCase(i.getEstado())
            && !"cancelado".equalsIgnoreCase(i.getEstado());
    }
}
