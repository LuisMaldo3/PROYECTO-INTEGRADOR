package pe.tecsup.vynk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VerificadorCredencial {

    private final JwtDecoder decoder;

    public VerificadorCredencial(
            @Value("${vynk.auth.jwks-uri}") String jwksUri,
            @Value("${vynk.auth.issuer}") String issuer,
            @Value("${vynk.auth.client-id}") String clientId) {

        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();

        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(clientId));

        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audiencia));

        this.decoder = nimbus;
    }

    public Jwt verificar(String idToken) {
        return decoder.decode(idToken); // lanza JwtException si no es válida
    }
}