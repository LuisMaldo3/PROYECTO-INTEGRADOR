package pe.tecsup.vynk.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.Inscripcion;
public interface InscripcionRepository extends MongoRepository<Inscripcion, String> {
    List<Inscripcion> findByEstudianteId(String estudianteId);
    List<Inscripcion> findByActividadId(String actividadId);
    Optional<Inscripcion> findByEstudianteIdAndActividadId(String estudianteId, String actividadId);
    @org.springframework.data.mongodb.repository.Query("{'integrantes': ?0, 'estado': 'inscrita'}")
    List<Inscripcion> buscarPorIntegrante(String estudianteId);
}
