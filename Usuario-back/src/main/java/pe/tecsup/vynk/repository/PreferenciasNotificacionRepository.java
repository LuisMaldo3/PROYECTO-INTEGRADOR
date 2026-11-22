package pe.tecsup.vynk.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.PreferenciasNotificacion;

public interface PreferenciasNotificacionRepository extends MongoRepository<PreferenciasNotificacion, String> {
}
