package pe.tecsup.vynk.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.DispositivoMovil;
import pe.tecsup.vynk.repository.DispositivoMovilRepository;
import pe.tecsup.vynk.repository.UsuarioRepository;

/** Guarda los celulares de cada estudiante para poder enviarles los recordatorios. */
@Service
public class DispositivosService {
    /** Forma de un token de FCM; sirve también para no guardar basura enviada por un cliente cualquiera. */
    public static final String PATRON_TOKEN = "[A-Za-z0-9:_\\-]{20,4096}";
    private static final Pattern TOKEN = Pattern.compile(PATRON_TOKEN);
    /** Más de esto casi seguro son celulares viejos: se olvidan los menos recientes. */
    static final int MAX_POR_USUARIO = 5;

    private final DispositivoMovilRepository dispositivos;
    private final UsuarioRepository usuarios;

    public DispositivosService(DispositivoMovilRepository dispositivos, UsuarioRepository usuarios) {
        this.dispositivos = dispositivos;
        this.usuarios = usuarios;
    }

    /** Registra o actualiza el celular de quien tiene la sesión; si el token era de otra cuenta, pasa a esta. */
    public void registrar(String sujeto, String token, Instant ahora) {
        String usuarioId = usuarioId(sujeto);
        String limpio = tokenValido(token);
        try {
            guardar(usuarioId, limpio, ahora);
        } catch (DuplicateKeyException e) {
            guardar(usuarioId, limpio, ahora); // dos registros simultáneos del mismo token: el segundo lo actualiza
        }
        olvidarSobrantes(usuarioId);
    }

    /** Olvida el celular al cerrar sesión, para que un aviso no le llegue a quien ya no usa la cuenta. */
    public void quitar(String sujeto, String token) {
        String usuarioId = usuarioId(sujeto);
        dispositivos.findByToken(tokenValido(token))
                .filter(d -> usuarioId.equals(d.getUsuarioId()))
                .ifPresent(d -> dispositivos.deleteById(d.getId()));
    }

    private void guardar(String usuarioId, String token, Instant ahora) {
        DispositivoMovil d = dispositivos.findByToken(token).orElseGet(() -> {
            DispositivoMovil nuevo = new DispositivoMovil();
            nuevo.setToken(token);
            nuevo.setCreadoEn(ahora);
            return nuevo;
        });
        d.setUsuarioId(usuarioId);
        d.setActualizadoEn(ahora);
        dispositivos.save(d);
    }

    private void olvidarSobrantes(String usuarioId) {
        List<DispositivoMovil> todos = dispositivos.findByUsuarioId(usuarioId);
        if (todos.size() <= MAX_POR_USUARIO) return;
        todos.stream()
                .sorted(Comparator.comparing(DispositivoMovil::getActualizadoEn,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .limit(todos.size() - MAX_POR_USUARIO)
                .forEach(d -> dispositivos.deleteById(d.getId()));
    }

    private String usuarioId(String sujeto) {
        if (sujeto == null || sujeto.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findByCorreo(sujeto).or(() -> usuarios.findById(sujeto))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (!usuario.isActivo()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return usuario.getId();
    }

    private static String tokenValido(String token) {
        String limpio = token == null ? "" : token.strip();
        if (!TOKEN.matcher(limpio).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del celular no es válido");
        return limpio;
    }
}
