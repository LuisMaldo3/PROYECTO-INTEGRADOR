package pe.tecsup.vynk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import pe.tecsup.vynk.dto.LoginResponse;
import pe.tecsup.vynk.dto.UsuarioDto;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.UsuarioRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final VerificadorCredencial verificador;
    private final UsuarioRepository repo;
    private final JwtEncoder encoder;
    private final PasswordEncoder passwordEncoder;
    private final String dominio;
    private final long horas;
    private final String hashFalso; // para que el login con correo inexistente tarde lo mismo

    public AuthService(VerificadorCredencial verificador,
                       UsuarioRepository repo,
                       JwtEncoder encoder,
                       PasswordEncoder passwordEncoder,
                       @Value("${vynk.auth.dominio}") String dominio,
                       @Value("${vynk.jwt.horas}") long horas) {
        this.verificador = verificador;
        this.repo = repo;
        this.encoder = encoder;
        this.passwordEncoder = passwordEncoder;
        this.dominio = dominio;
        this.horas = horas;
        this.hashFalso = passwordEncoder.encode("vynk-hash-falso");
    }

    /** Login con Google: valida el ID token y crea el usuario la primera vez. */
    public LoginResponse login(String idToken) {
        Jwt credencial;
        try {
            credencial = verificador.verificar(idToken);
        } catch (JwtException e) {
            throw new CredencialInvalidaException();
        }

        String correo = credencial.getClaimAsString("email");
        if (correo == null || !correo.toLowerCase().endsWith(dominio)) {
            throw new DominioNoPermitidoException(); // regla de US-02
        }
        String correoFinal = correo.toLowerCase();

        Usuario usuario = repo.findByCorreo(correoFinal).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setCorreo(correoFinal);
            nuevo.setNombre(credencial.getClaimAsString("name"));
            return repo.save(nuevo); // rol "usuario" por defecto
        });

        return emitir(usuario);
    }

    /**
     * Login con correo y contraseña. Solo funciona para usuarios que ya
     * entraron con Google y definieron una contraseña (ver definirPassword).
     */
    public LoginResponse loginConPassword(String correo, String password) {
        Usuario usuario = repo.findByCorreo(correo.trim().toLowerCase()).orElse(null);
        String hash = (usuario != null) ? usuario.getPasswordHash() : null;

        boolean coincide = passwordEncoder.matches(password, hash != null ? hash : hashFalso);
        if (usuario == null || hash == null || !coincide || !usuario.isActivo()) {
            throw new LoginIncorrectoException();
        }
        return emitir(usuario);
    }

    /** Crea o cambia la contraseña del usuario autenticado (id = subject del JWT). */
    public void definirPassword(String usuarioId, String password) {
        Usuario usuario = repo.findById(usuarioId).orElseThrow(CredencialInvalidaException::new);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        repo.save(usuario);
    }

    private LoginResponse emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getId())
                .claim("correo", usuario.getCorreo())
                .claim("rol", usuario.getRol())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(horas, ChronoUnit.HOURS))
                .build();

        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        return new LoginResponse(token, new UsuarioDto(
                usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol()));
    }
}
