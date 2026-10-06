package pe.tecsup.vynk.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByCorreo(String correo);
}