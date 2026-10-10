package pe.tecsup.vynk.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Un token de sesión que ya no sirve porque la persona cerró sesión.
 * El id es el identificador del token (claim "jti"); {@code expira} es cuándo vencería
 * de todos modos: pasada esa fecha ya no hace falta recordarlo.
 */
@Document("tokens_revocados")
public class TokenRevocado {

    @Id
    private String id;

    private Instant expira;

    public TokenRevocado() {
    }

    public TokenRevocado(String id, Instant expira) {
        this.id = id;
        this.expira = expira;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getExpira() { return expira; }
    public void setExpira(Instant expira) { this.expira = expira; }
}
