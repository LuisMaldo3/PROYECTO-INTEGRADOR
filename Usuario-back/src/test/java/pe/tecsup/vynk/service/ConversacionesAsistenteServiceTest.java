package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import pe.tecsup.vynk.model.ConversacionAsistente;

class ConversacionesAsistenteServiceTest {
    @Test void asociaHistorialExclusivamenteAlIdDelJwt() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        ConversacionesAsistenteService service = new ConversacionesAsistenteService(mongo);
        service.obtener("usuario-uno");
        verify(mongo).findById("usuario-uno", ConversacionAsistente.class);
        verify(mongo, never()).findById("usuario-dos", ConversacionAsistente.class);
    }

    @Test void borradoInvalidaRevisionYNoReintroduceUnaRespuestaAntigua() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        ConversacionesAsistenteService service = new ConversacionesAsistenteService(mongo);
        var pregunta = ConversacionAsistente.Mensaje.usuario("Hola");
        var respuesta = ConversacionAsistente.Mensaje.usuario("Contestación");
        assertFalse(service.guardarIntercambio("u1", "revision-antigua", pregunta, respuesta));
        verify(mongo).findAndModify(any(Query.class), any(Update.class), eq(ConversacionAsistente.class));
        service.borrar("u1");
        verify(mongo).updateFirst(any(Query.class), any(Update.class), eq(ConversacionAsistente.class));
    }
}
