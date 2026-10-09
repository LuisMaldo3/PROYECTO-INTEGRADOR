package pe.tecsup.vynk.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolesTest {

    @Test
    void estanLosTresRolesDelProyecto() {
        assertEquals(java.util.List.of("usuario", "organizador", "administrador"), Roles.TODOS);
    }

    @Test
    void esValido_aceptaRolesConocidos() {
        assertTrue(Roles.esValido("usuario"));
        assertTrue(Roles.esValido("  Organizador "));
        assertTrue(Roles.esValido("ADMINISTRADOR"));
    }

    @Test
    void esValido_rechazaLoDemas() {
        assertFalse(Roles.esValido("superadmin"));
        assertFalse(Roles.esValido(""));
        assertFalse(Roles.esValido(null));
    }

    @Test
    void unUsuarioNuevoEmpiezaComoUsuario() {
        assertEquals(Roles.USUARIO, new Usuario().getRol());
    }
}
