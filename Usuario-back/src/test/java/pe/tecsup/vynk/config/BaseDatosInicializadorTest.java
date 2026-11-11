package pe.tecsup.vynk.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class BaseDatosInicializadorTest {

    @Test
    void creaColeccionesYCatalogosSinSobrescribirRegistros() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.collectionExists(anyString())).thenReturn(false);

        new BaseDatosInicializador(mongo, true).run(new DefaultApplicationArguments());

        verify(mongo).executeCommand(new Document("ping", 1));
        verify(mongo, times(BaseDatosInicializador.COLECCIONES.size())).createCollection(anyString());
        verify(mongo).createCollection("comentarios_actividades");
        verify(mongo).createCollection("resultados_competencias");
        verify(mongo, never()).createCollection("asistencias");

        ArgumentCaptor<Query> consultas = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> cambios = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<String> colecciones = ArgumentCaptor.forClass(String.class);
        verify(mongo, times(7)).upsert(consultas.capture(), cambios.capture(), colecciones.capture());
        assertEquals(5, colecciones.getAllValues().stream().filter("areas_tecsup"::equals).count());
        assertEquals(2, colecciones.getAllValues().stream().filter("categorias"::equals).count());
        assertEquals(7, consultas.getAllValues().stream()
                .filter(q -> q.getQueryObject().containsKey("_id")).count());
        for (Update cambio : cambios.getAllValues()) {
            Document documento = cambio.getUpdateObject();
            assertTrue(documento.containsKey("$setOnInsert"));
            assertFalse(documento.containsKey("$set"));
            assertFalse(documento.containsKey("$unset"));
        }
    }

    @Test
    void conColeccionesExistentesNoIntentaRecrearlas() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.collectionExists(anyString())).thenReturn(true);

        new BaseDatosInicializador(mongo, true).run(new DefaultApplicationArguments());

        verify(mongo, never()).createCollection(anyString());
        verify(mongo, times(7)).upsert(any(Query.class), any(Update.class), anyString());
    }

    @Test
    void puedeDesactivarseExpresamente() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        new BaseDatosInicializador(mongo, false).run(new DefaultApplicationArguments());
        verifyNoInteractions(mongo);
    }

    @Test
    void fallaVisiblementeSiMongoNoEstaDisponible() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.executeCommand(any(Document.class)))
                .thenThrow(new IllegalStateException("MongoDB no disponible"));
        assertThrows(IllegalStateException.class, () ->
                new BaseDatosInicializador(mongo, true).run(new DefaultApplicationArguments()));
        verify(mongo, never()).createCollection(anyString());
    }

    @Test
    void lasAreasSonExactamenteLasCincoInstitucionales() {
        assertEquals(List.of("Gestión y Producción", "Tecnología Digital", "Minería y Procesos Químicos",
                "Electricidad y Electrónica", "Mecánica"), BaseDatosInicializador.AREAS);
    }
}
