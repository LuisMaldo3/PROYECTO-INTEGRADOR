package pe.tecsup.vynk.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.RecomendacionDescartada;

public interface RecomendacionDescartadaRepository extends MongoRepository<RecomendacionDescartada, String> {
    List<RecomendacionDescartada> findByUsuarioId(String usuarioId);
}
