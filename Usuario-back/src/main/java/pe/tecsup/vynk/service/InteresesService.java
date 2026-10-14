package pe.tecsup.vynk.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.Interes;
import pe.tecsup.vynk.model.InteresesDeUsuario;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.InteresRepository;
import pe.tecsup.vynk.repository.InteresesDeUsuarioRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Intereses: la lista de los disponibles y lo que eligió cada persona. */
@Service
public class InteresesService {

    private final InteresRepository catalogo;
    private final InteresesDeUsuarioRepository elegidos;
    private final UsuarioRepository usuarios;

    public InteresesService(InteresRepository catalogo, InteresesDeUsuarioRepository elegidos, UsuarioRepository usuarios) {
        this.catalogo = catalogo;
        this.elegidos = elegidos;
        this.usuarios = usuarios;
    }

    /** Los intereses disponibles, en el orden en que se muestran. */
    public List<Interes> disponibles() {
        return catalogo.findAll(Sort.by("orden"));
    }

    /** Lo que eligió la persona (vacío si todavía no eligió nada). */
    public InteresesDeUsuario deUsuario(String usuarioId) {
        exigirCuentaActiva(usuarioId);
        return elegidos.findById(usuarioId).orElseGet(() -> new InteresesDeUsuario(usuarioId, List.of(), null));
    }

    /** Valida y guarda lo que eligió la persona (reemplaza lo anterior). */
    public InteresesDeUsuario guardar(String usuarioId, List<String> pedidos) {
        exigirCuentaActiva(usuarioId);
        Set<String> validos = disponibles().stream().map(Interes::getId).collect(Collectors.toSet());
        List<String> limpios = validarSeleccion(pedidos, validos);
        return elegidos.save(new InteresesDeUsuario(usuarioId, limpios, Instant.now()));
    }

    private void exigirCuentaActiva(String usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(CredencialInvalidaException::new);
        if (!usuario.isActivo()) {
            throw new CredencialInvalidaException();
        }
    }

    /**
     * Revisa la lista enviada y devuelve los ids limpios (en minúsculas, sin repetir y en el orden en que
     * llegaron). Lanza SeleccionInvalidaException si falta la lista, queda vacía o trae un interés que no existe.
     * Es lógica pura (sin base de datos) para poder probarla sola.
     */
    public static List<String> validarSeleccion(List<String> pedidos, Set<String> validos) {
        if (pedidos == null) {
            throw new SeleccionInvalidaException("Envía la lista de intereses.");
        }
        Set<String> limpios = new LinkedHashSet<>();
        for (String pedido : pedidos) {
            String id = pedido == null ? "" : pedido.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            if (!validos.contains(id)) {
                throw new SeleccionInvalidaException("El interés «" + id + "» no existe.");
            }
            limpios.add(id);
        }
        if (limpios.isEmpty()) {
            throw new SeleccionInvalidaException("Elige al menos un interés.");
        }
        return List.copyOf(limpios);
    }
}
