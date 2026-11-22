package pe.tecsup.vynk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * US-72: las actividades que comienzan próximamente. Toma las programadas que todavía no empiezan, de la más cercana
 * a la más lejana, y agrega cuándo empiezan exactamente («empiezaEn», para que la app cuente los minutos con su
 * propio reloj), cuántos minutos faltan y si el estudiante está inscrito.
 */
@Service
public class ProximasActividadesService {
    static final int POR_DEFECTO = 3;
    static final int MAXIMO = 10;

    private final CatalogoController catalogo;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public ProximasActividadesService(CatalogoController catalogo, InscripcionRepository inscripciones, UsuarioRepository usuarios) {
        this.catalogo = catalogo;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    public Map<String, Object> proximas(String sujeto, Integer limite, Instant ahora) {
        String usuarioId = usuarioId(sujeto);
        int cuantas = limite == null ? POR_DEFECTO : limite;
        if (cuantas < 1 || cuantas > MAXIMO)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El límite debe estar entre 1 y " + MAXIMO);
        Set<String> mias = new HashSet<>();
        for (Inscripcion i : inscripciones.findByEstudianteId(usuarioId))
            if ("inscrita".equalsIgnoreCase(i.getEstado())) mias.add(i.getActividadId());
        for (Inscripcion i : inscripciones.buscarPorIntegrante(usuarioId)) mias.add(i.getActividadId());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> publicadas = (List<Map<String, Object>>) catalogo.listar().get("actividades");
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> a : elegir(publicadas, ahora, cuantas)) {
            Instant inicio = SelectorRecomendaciones.inicio(a);
            Map<String, Object> item = new LinkedHashMap<>(a);
            item.put("empiezaEn", inicio.toString());
            item.put("minutosParaEmpezar", minutos(ahora, inicio));
            item.put("inscrito", mias.contains(String.valueOf(a.get("id"))));
            salida.add(item);
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("ahora", ahora.toString());
        respuesta.put("total", salida.size());
        respuesta.put("actividades", salida);
        return respuesta;
    }

    /** Las [cuantas] que empiezan antes, sin contar las que ya empezaron ni las que no tienen fecha y hora válidas. */
    static List<Map<String, Object>> elegir(List<Map<String, Object>> publicadas, Instant ahora, int cuantas) {
        if (publicadas == null) return List.of();
        return publicadas.stream()
                .filter(a -> a != null && "programada".equalsIgnoreCase(String.valueOf(a.getOrDefault("estado", "programada"))))
                .filter(a -> { Instant i = SelectorRecomendaciones.inicio(a); return i != null && i.isAfter(ahora); })
                .sorted(Comparator.comparing((Map<String, Object> a) -> SelectorRecomendaciones.inicio(a))
                        .thenComparing(a -> String.valueOf(a.get("nombre"))))
                .limit(cuantas)
                .toList();
    }

    /** Minutos que faltan, redondeados hacia arriba: a los 30 segundos de empezar todavía dice «1 min». */
    static long minutos(Instant ahora, Instant inicio) {
        return Math.max(1, (Duration.between(ahora, inicio).toSeconds() + 59) / 60);
    }

    private String usuarioId(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(sujeto).or(() -> usuarios.findById(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario.getId();
    }
}
