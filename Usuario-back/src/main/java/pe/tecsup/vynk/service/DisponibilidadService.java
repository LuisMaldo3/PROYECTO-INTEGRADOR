package pe.tecsup.vynk.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.DisponibilidadSemanal;
import pe.tecsup.vynk.model.DisponibilidadSemanal.BloqueOcupado;
import pe.tecsup.vynk.repository.DisponibilidadRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

@Service
public class DisponibilidadService {
    private final DisponibilidadRepository repo;
    private final UsuarioRepository usuarios;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    public DisponibilidadService(DisponibilidadRepository repo, UsuarioRepository usuarios) {
        this.repo = repo; this.usuarios = usuarios;
    }
    public String usuarioId(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(sujeto).or(() -> usuarios.findByCorreo(sujeto))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario.getId();
    }
    public DisponibilidadSemanal obtener(String sujeto) {
        String id = usuarioId(sujeto);
        return repo.findByUsuarioId(id).orElseGet(() -> {
            DisponibilidadSemanal nuevo = new DisponibilidadSemanal();
            nuevo.setUsuarioId(id); return nuevo;
        });
    }
    public DisponibilidadSemanal guardar(String sujeto, List<BloqueOcupado> bloques) {
        DisponibilidadSemanal actual = obtener(sujeto);
        actual.setBloques(normalizar(bloques));
        actual.setConfirmado(true);
        return repo.save(actual);
    }
    /** Valida los datos generados por IA o corregidos por el usuario; nunca confía en su texto. */
    public static List<BloqueOcupado> normalizar(List<BloqueOcupado> bloques) {
        if (bloques == null || bloques.size() > 100)
            throw new IllegalArgumentException("El horario debe contener un máximo de 100 bloques.");
        List<BloqueOcupado> limpio = new ArrayList<>();
        for (BloqueOcupado b : bloques) {
            if (b == null || b.dia() < 1 || b.dia() > 7)
                throw new IllegalArgumentException("Día inválido: usa números del 1 (lunes) al 7 (domingo).");
            try {
                LocalTime ini = LocalTime.parse(b.inicio(), HHMM);
                LocalTime fin = LocalTime.parse(b.fin(), HHMM);
                if (!ini.isBefore(fin)) throw new IllegalArgumentException("La hora final debe ser mayor que la inicial.");
                String nombre = b.nombre() == null ? "Ocupado" : b.nombre().strip();
                if (nombre.length() > 80) nombre = nombre.substring(0, 80);
                limpio.add(new BloqueOcupado(b.dia(), ini.format(HHMM), fin.format(HHMM), nombre));
            } catch (java.time.format.DateTimeParseException | NullPointerException e) {
                throw new IllegalArgumentException("Las horas deben tener formato HH:mm válido.");
            }
        }
        limpio.sort(Comparator.comparingInt(BloqueOcupado::dia).thenComparing(BloqueOcupado::inicio));
        // Mantener las filas tal como el usuario las confirmó; cruces se combinan al calcular huecos.
        return List.copyOf(limpio);
    }
}
