package pe.tecsup.vynk.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.function.Predicate;

/** Rechaza (401) un token cuya sesión ya fue cerrada, aunque todavía no haya vencido. */
public class ValidadorTokenRevocado implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESION_CERRADA =
            new OAuth2Error("invalid_token", "La sesión fue cerrada.", null);

    private final Predicate<String> estaRevocado;

    /** @param estaRevocado recibe el identificador (jti) del token y dice si fue revocado */
    public ValidadorTokenRevocado(Predicate<String> estaRevocado) {
        this.estaRevocado = estaRevocado;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String jti = jwt.getId();
        if (jti != null && estaRevocado.test(jti)) {
            return OAuth2TokenValidatorResult.failure(SESION_CERRADA);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
