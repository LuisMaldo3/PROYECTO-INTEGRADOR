package pe.tecsup.vynk.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.AlertaUbicacion;

public interface AlertaUbicacionRepository extends MongoRepository<AlertaUbicacion, String> {
    /** Alertas por enviar, las más antiguas primero; el tope evita ciclos eternos tras una caída larga. */
    List<AlertaUbicacion> findTop200ByEstadoOrderByCreadaEnAsc(String estado);
    List<AlertaUbicacion> findByInscripcionId(String inscripcionId);
}
