package pe.tecsup.vynk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.DispositivoMovil;

public interface DispositivoMovilRepository extends MongoRepository<DispositivoMovil, String> {
    List<DispositivoMovil> findByUsuarioId(String usuarioId);
    Optional<DispositivoMovil> findByToken(String token);
    void deleteByToken(String token);
}
