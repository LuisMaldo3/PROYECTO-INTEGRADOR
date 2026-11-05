package pe.tecsup.vynk.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.RecomendacionDescartada;
import pe.tecsup.vynk.repository.RecomendacionDescartadaRepository;

/** Almacena las preferencias de desinteres en el servidor, por usuario autenticado. */
@Service
public class DescartesRecomendacionesService {
    private final RecomendacionDescartadaRepository repositorio;
    public DescartesRecomendacionesService(RecomendacionDescartadaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void descartar(String usuarioId, String actividadId) {
        if (usuarioId == null || usuarioId.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión.");
        if (actividadId == null || !actividadId.matches("[A-Za-z0-9_-]{1,128}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID de actividad inválido.");
        // El identificador es estable: repetir el POST no duplica el descarte.
        repositorio.save(new RecomendacionDescartada(usuarioId, actividadId));
    }

    public Set<String> idsDescartados(String usuarioId) {
        return repositorio.findByUsuarioId(usuarioId).stream()
                .map(RecomendacionDescartada::getActividadId).collect(Collectors.toUnmodifiableSet());
    }

    public static List<Map<String, Object>> excluir(List<Map<String, Object>> candidatas, Set<String> ids) {
        if (candidatas == null) return List.of();
        if (ids == null || ids.isEmpty()) return List.copyOf(candidatas);
        return candidatas.stream()
                .filter(a -> a != null && !ids.contains(String.valueOf(a.get("id")))).toList();
    }
}
