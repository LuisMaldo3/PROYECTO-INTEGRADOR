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
    private final ValidadorCorreoInstitucional validadorCorreo;
    private final UsuarioRepository repo;
    private final JwtEncoder encoder;
    private final PasswordEncoder passwordEncoder;
    private final long horas;
    private final String hashFalso;

    public AuthService(VerificadorCredencial verificador,
                       ValidadorCorreoInstitucional validadorCorreo,
                       UsuarioRepository repo,
                       JwtEncoder encoder,
                       PasswordEncoder passwordEncoder,
                       @Value("${vynk.jwt.horas}") long horas) {
        this.verificador = verificador;
        this.validadorCorreo = validadorCorreo;
        this.repo = repo;
        this.encoder = encoder;
        this.passwordEncoder = passwordEncoder;
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

        // Google debe haber verificado el correo; si no, cualquiera podria declarar uno ajeno.
        if (!Boolean.TRUE.equals(credencial.getClaimAsBoolean("email_verified"))) {
            throw new CredencialInvalidaException();
        }

        // US-02 (Mishel): leer correo del token y revisar dominio Tecsup
        String correoFinal = exigirCorreoInstitucional(credencial.getClaimAsString("email"));

        Usuario usuario = repo.findByCorreo(correoFinal).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setCorreo(correoFinal);
            nuevo.setNombre(credencial.getClaimAsString("name"));
            return repo.save(nuevo);
        });

        // Igual que en el login con contrasena: una cuenta desactivada no entra.
        if (!usuario.isActivo()) {
            throw new CredencialInvalidaException();
        }

        return emitir(usuario);
    }

    /** Login con correo y contraseña. */
    public LoginResponse loginConPassword(String correo, String password) {
        String correoFinal = exigirCorreoInstitucional(correo);

        Usuario usuario = repo.findByCorreo(correoFinal).orElse(null);
        String hash = (usuario != null) ? usuario.getPasswordHash() : null;

        boolean coincide = passwordEncoder.matches(password, hash != null ? hash : hashFalso);
        if (usuario == null || hash == null || !coincide || !usuario.isActivo()) {
            throw new LoginIncorrectoException();
        }
        return emitir(usuario);
    }

    /** Lee el correo y deja pasar solo cuentas @tecsup.edu.pe. */
    private String exigirCorreoInstitucional(String correoCrudo) {
        String correo = validadorCorreo.leerCorreo(correoCrudo);
        if (!validadorCorreo.esDeTecsup(correo)) {
            throw new DominioNoPermitidoException();
        }
        return correo;
    }

    /** Crea o cambia la contraseña del usuario autenticado. */
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