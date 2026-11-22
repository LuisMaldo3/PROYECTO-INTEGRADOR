package pe.tecsup.vynk.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.AlertaHorario;

public interface AlertaHorarioRepository extends MongoRepository<AlertaHorario, String> {
    /** Alertas por enviar, las más antiguas primero; el tope evita ciclos eternos tras una caída larga. */
    List<AlertaHorario> findTop200ByEstadoOrderByCreadaEnAsc(String estado);
    List<AlertaHorario> findByInscripcionId(String inscripcionId);
}
