package pe.tecsup.vynk.service;

import org.springframework.beans.factory.annotation.Value;
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
    private final String dominio;
    private final long horas;

    public AuthService(VerificadorCredencial verificador,
                       UsuarioRepository repo,
                       JwtEncoder encoder,
                       @Value("${vynk.auth.dominio}") String dominio,
                       @Value("${vynk.jwt.horas}") long horas) {
        this.verificador = verificador;
        this.repo = repo;
        this.encoder = encoder;
        this.dominio = dominio;
        this.horas = horas;
    }

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