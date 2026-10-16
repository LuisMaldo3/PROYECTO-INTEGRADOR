package pe.tecsup.vynk.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.DisponibilidadSemanal;

public interface DisponibilidadRepository extends MongoRepository<DisponibilidadSemanal, String> {
    Optional<DisponibilidadSemanal> findByUsuarioId(String usuarioId);
}
