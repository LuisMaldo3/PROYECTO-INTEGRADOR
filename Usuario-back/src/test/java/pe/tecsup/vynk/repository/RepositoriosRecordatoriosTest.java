package pe.tecsup.vynk.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

/**
 * Spring Data revisa los nombres de las consultas (findBy...) al crear cada repositorio; un nombre mal escrito
 * hace que el servidor no arranque. Aquí se crean sin conectarse a ninguna base de datos para detectarlo en las pruebas.
 */
class RepositoriosRecordatoriosTest {
    @Test void losNombresDeLasConsultasCorrespondenSusCampos() {
        try (var cliente = MongoClients.create("mongodb://localhost:1/vynk?serverSelectionTimeoutMS=100")) {
            var plantilla = new MongoTemplate(new SimpleMongoClientDatabaseFactory(cliente, "vynk"));
            var fabrica = new MongoRepositoryFactory(plantilla);
            assertNotNull(fabrica.getRepository(RecordatorioActividadRepository.class));
            assertNotNull(fabrica.getRepository(DispositivoMovilRepository.class));
            assertNotNull(fabrica.getRepository(InscripcionRepository.class));
            assertNotNull(fabrica.getRepository(AlertaHorarioRepository.class));
            assertNotNull(fabrica.getRepository(AlertaUbicacionRepository.class));
        }
    }
}
