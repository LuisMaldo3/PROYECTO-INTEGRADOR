package pe.tecsup.vynk.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.Rol;

public interface RolRepository extends MongoRepository<Rol, String> {
}
