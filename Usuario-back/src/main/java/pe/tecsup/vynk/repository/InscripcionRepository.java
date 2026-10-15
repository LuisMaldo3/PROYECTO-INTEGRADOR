package pe.tecsup.vynk.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.Inscripcion;
public interface InscripcionRepository extends MongoRepository<Inscripcion, String> {
    List<Inscripcion> findByEstudianteId(String estudianteId);
    List<Inscripcion> findByActividadId(String actividadId);
}
