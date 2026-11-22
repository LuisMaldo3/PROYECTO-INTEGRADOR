package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.controller.CatalogoController;
import pe.tecsup.vynk.model.Inscripcion;
import pe.tecsup.vynk.repository.InscripcionRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/**
 * US-74: dónde es mi próxima actividad. De las actividades en las que el estudiante está inscrito, elige la que está
 * en curso o, si no hay ninguna, la que empieza antes, y la entrega con su ficha de ubicación (lugar, referencia,
 * pabellón, coordenadas si existen). También dice cuál viene después, para planificar el traslado.
 */
@Service
public class MiProximaActividadService {
    /** Si una actividad no tiene hora de fin, se considera en curso durante una hora. */
    static final Duration DURACION_SIN_FIN = Duration.ofHours(1);

    private final CatalogoController catalogo;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public MiProximaActividadService(CatalogoController catalogo, InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.catalogo = catalogo;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    record Tramo(Inscripcion inscripcion, Instant inicio, Instant fin) {}

    public Map<String, Object> obtener(String sujeto, Instant ahora) {
        String usuarioId = usuarioId(sujeto);
        Set<String> vistas = new LinkedHashSet<>();
        List<Inscripcion> mias = new ArrayList<>();
        for (Inscripcion i : inscripciones.findByEstudianteId(usuarioId))
            if ("inscrita".equalsIgnoreCase(i.getEstado()) && vistas.add(i.getActividadId())) mias.add(i);
        for (Inscripcion i : inscripciones.buscarPorIntegrante(usuarioId))
            if (vistas.add(i.getActividadId())) mias.add(i);

        List<Map<String, Object>> elegidas = new ArrayList<>();
        List<Tramo> orden = ordenar(mias, ahora);
        for (Tramo t : orden) {
            if (elegidas.size() == 2) break;
            Map<String, Object> actividad;
            try {
                actividad = catalogo.detalle(t.inscripcion().getActividadId());
            } catch (ResponseStatusException e) {
                continue; // cancelada o retirada del catálogo: no es «mi próxima actividad»
            }
            if (!"programada".equalsIgnoreCase(String.valueOf(actividad.get("estado")))) continue;
            Map<String, Object> item = new LinkedHashMap<>(actividad);
            boolean enCurso = !t.inicio().isAfter(ahora);
            item.put("empiezaEn", t.inicio().toString());
            item.put("terminaEn", t.fin().toString());
            item.put("enCurso", enCurso);
            item.put("minutosParaEmpezar", enCurso ? 0L : ProximasActividadesService.minutos(ahora, t.inicio()));
            elegidas.add(item);
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("ahora", ahora.toString());
        respuesta.put("tiene", !elegidas.isEmpty());
        respuesta.put("actividad", elegidas.isEmpty() ? null : elegidas.get(0));
        if (elegidas.size() > 1) {
            Map<String, Object> despues = elegidas.get(1);
            Map<String, Object> resumen = new LinkedHashMap<>();
            for (String campo : List.of("id", "nombre", "fecha", "horaInicio", "horaFin", "ubicacion", "empiezaEn"))
                resumen.put(campo, despues.get(campo));
            respuesta.put("despues", resumen);
        } else {
            respuesta.put("despues", null);
        }
        return respuesta;
    }

    /** En curso primero y después por hora de inicio; descarta las que ya terminaron o no tienen fecha y hora. */
    static List<Tramo> ordenar(List<Inscripcion> inscripciones, Instant ahora) {
        List<Tramo> tramos = new ArrayList<>();
        for (Inscripcion i : inscripciones) {
            Instant inicio = ProgramadorRecordatorios.inicioDe(i);
            if (inicio == null) continue;
            Instant fin = Horario.de(i.getFecha(), i.getHoraFin(), null).comienzo();
            if (fin == null || !fin.isAfter(inicio)) fin = inicio.plus(DURACION_SIN_FIN);
            if (!fin.isAfter(ahora)) continue;
            tramos.add(new Tramo(i, inicio, fin));
        }
        tramos.sort(Comparator.comparing(Tramo::inicio));
        return tramos;
    }

    private String usuarioId(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(sujeto).or(() -> usuarios.findById(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario.getId();
    }
}
