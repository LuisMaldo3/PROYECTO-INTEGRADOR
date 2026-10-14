package pe.tecsup.vynk.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.Interes;

public interface InteresRepository extends MongoRepository<Interes, String> {
}
