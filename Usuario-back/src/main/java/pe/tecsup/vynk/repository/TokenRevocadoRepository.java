package pe.tecsup.vynk.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.TokenRevocado;

import java.time.Instant;

public interface TokenRevocadoRepository extends MongoRepository<TokenRevocado, String> {

    /** Borra los que ya vencieron: un token vencido se rechaza solo, no hace falta guardarlo. */
    void deleteByExpiraBefore(Instant instante);
}
