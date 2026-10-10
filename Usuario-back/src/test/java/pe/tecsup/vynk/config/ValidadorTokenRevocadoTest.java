package pe.tecsup.vynk.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorTokenRevocadoTest {

    private static Jwt tokenCon(String jti) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").claim("sub", "usuario-1");
        if (jti != null) {
            builder.claim("jti", jti);
        }
        return builder.build();
    }

    private final ValidadorTokenRevocado validador = new ValidadorTokenRevocado(Set.of("revocado-1")::contains);

    @Test
    void rechazaElTokenDeUnaSesionCerrada() {
        OAuth2TokenValidatorResult resultado = validador.validate(tokenCon("revocado-1"));
        assertTrue(resultado.hasErrors());
    }

    @Test
    void aceptaUnTokenQueNoFueRevocado() {
        assertFalse(validador.validate(tokenCon("otro-token")).hasErrors());
    }

    @Test
    void aceptaUnTokenSinIdentificador_comoLosEmitidosAntes() {
        // No consulta la lista: no hay nada que buscar.
        ValidadorTokenRevocado estricto = new ValidadorTokenRevocado(jti -> {
            throw new AssertionError("no debe consultarse sin jti");
        });
        assertFalse(estricto.validate(tokenCon(null)).hasErrors());
    }
}
