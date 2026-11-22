package pe.tecsup.vynk.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.RecordatorioActividad;

public interface RecordatorioActividadRepository extends MongoRepository<RecordatorioActividad, String> {
    Optional<RecordatorioActividad> findByInscripcionId(String inscripcionId);
    /** Avisos que ya toca enviar, los más atrasados primero; el tope evita ciclos eternos tras una caída larga. */
    List<RecordatorioActividad> findTop200ByEstadoAndEnviarEnLessThanEqualOrderByEnviarEnAsc(String estado, Instant limite);
}
