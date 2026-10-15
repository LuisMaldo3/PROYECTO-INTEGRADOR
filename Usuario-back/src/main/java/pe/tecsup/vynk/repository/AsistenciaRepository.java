package pe.tecsup.vynk.repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.tecsup.vynk.model.Asistencia;
public interface AsistenciaRepository extends MongoRepository<Asistencia, String> {
    List<Asistencia> findByActividadId(String actividadId);
}
