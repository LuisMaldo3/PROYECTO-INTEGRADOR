package pe.tecsup.vynk.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.InteresesDeUsuario;

public interface InteresesDeUsuarioRepository extends MongoRepository<InteresesDeUsuario, String> {
}
