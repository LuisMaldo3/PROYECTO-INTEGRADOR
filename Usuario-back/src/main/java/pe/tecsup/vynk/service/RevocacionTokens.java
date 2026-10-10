package pe.tecsup.vynk.service;

import org.springframework.stereotype.Service;
import pe.tecsup.vynk.model.TokenRevocado;
import pe.tecsup.vynk.repository.TokenRevocadoRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Cierre de sesión en el servidor: recuerda qué tokens ya no deben aceptarse. */
@Service
public class RevocacionTokens {

    private final TokenRevocadoRepository repo;

    public RevocacionTokens(TokenRevocadoRepository repo) {
        this.repo = repo;
    }

    /**
     * Marca el token como revocado. {@code jti} es su identificador y {@code expira} cuándo vence.
     * Los tokens emitidos antes de que existiera esta función no traen identificador: no se pueden
     * revocar y simplemente vencen a su hora.
     */
    public void revocar(String jti, Instant expira) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        Instant ahora = Instant.now();
        repo.deleteByExpiraBefore(ahora); // limpieza: lo vencido ya se rechaza solo
        repo.save(new TokenRevocado(jti, expira != null ? expira : ahora.plus(1, ChronoUnit.DAYS)));
    }

    public boolean estaRevocado(String jti) {
        return jti != null && repo.existsById(jti);
    }
}
