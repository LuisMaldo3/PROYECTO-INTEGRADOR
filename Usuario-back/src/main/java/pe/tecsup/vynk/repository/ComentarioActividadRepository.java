package pe.tecsup.vynk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.ComentarioActividad;

public interface ComentarioActividadRepository extends MongoRepository<ComentarioActividad, String> {
    Optional<ComentarioActividad> findByActividadIdAndEstudianteId(String actividadId, String estudianteId);
    List<ComentarioActividad> findByActividadIdOrderByActualizadoEnDesc(String actividadId);
    long countByActividadId(String actividadId);
}
