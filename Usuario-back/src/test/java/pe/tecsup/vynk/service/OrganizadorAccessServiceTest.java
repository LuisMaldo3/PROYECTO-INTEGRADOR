package pe.tecsup.vynk.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import pe.tecsup.vynk.model.Usuario;
import pe.tecsup.vynk.repository.UsuarioRepository;

class OrganizadorAccessServiceTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizadorAccessService servicio = new OrganizadorAccessService(mongo, usuarios);
    private final Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
            Map.of("alg", "HS256"), Map.of("sub", "organizador-1"));

    private Usuario usuario(String rol, boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId("organizador-1");
        usuario.setRol(rol);
        usuario.setActivo(activo);
        return usuario;
    }

    @Test void exigeRolActualYCuentaActiva() {
        when(usuarios.findById("organizador-1")).thenReturn(Optional.of(usuario("usuario", true)));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class, () -> servicio.exigirOrganizador(jwt)).getStatusCode());
        when(usuarios.findById("organizador-1")).thenReturn(Optional.of(usuario("organizador", false)));
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class, () -> servicio.exigirOrganizador(jwt)).getStatusCode());
        when(usuarios.findById("organizador-1")).thenReturn(Optional.of(usuario("organizador", true)));
        assertEquals("organizador-1", servicio.exigirOrganizador(jwt).getId());
    }

    @Test void nuncaEntregaActividadesDeOtroOrganizador() {
        ObjectId id = new ObjectId();
        Document actividad = new Document("_id", id).append("organizadorId", "otro-organizador");
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("actividades"))).thenReturn(actividad);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> servicio.exigirActividadPropia(usuario("organizador", true), id.toHexString())).getStatusCode());
        actividad.put("organizadorId", "organizador-1");
        assertSame(actividad, servicio.exigirActividadPropia(usuario("organizador", true), id.toHexString()));
    }
}
