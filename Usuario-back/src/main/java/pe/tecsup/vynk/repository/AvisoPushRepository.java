package pe.tecsup.vynk.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.AvisoPush;

public interface AvisoPushRepository extends MongoRepository<AvisoPush, String> {
    /** Por enviar, los más antiguos primero; el tope evita ciclos eternos tras una caída larga. */
    List<AvisoPush> findTop200ByEstadoOrderByCreadoEnAsc(String estado);
    boolean existsByClave(String clave);
    /** Cuántos avisos de un tipo se le generaron a un estudiante desde un momento (por ejemplo, hoy). */
    long countByUsuarioIdAndTipoAndCreadoEnGreaterThanEqual(String usuarioId, String tipo, Instant desde);
    List<AvisoPush> findByUsuarioIdAndTipo(String usuarioId, String tipo);
}
