package pe.tecsup.vynk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorCorreoInstitucionalTest {

    private ValidadorCorreoInstitucional validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorCorreoInstitucional("@tecsup.edu.pe");
    }

    @Test
    void leerCorreo_normalizaEspaciosYMayusculas() {
        assertEquals("alumno@tecsup.edu.pe",
                validador.leerCorreo("  Alumno@Tecsup.edu.pe  "));
    }

    @Test
    void leerCorreo_rechazaVacio() {
        assertThrows(CredencialInvalidaException.class, () -> validador.leerCorreo("  "));
        assertThrows(CredencialInvalidaException.class, () -> validador.leerCorreo(null));
    }

    @Test
    void esDeTecsup_aceptaDominioInstitucional() {
        assertTrue(validador.esDeTecsup("alumno@tecsup.edu.pe"));
        assertTrue(validador.esDeTecsup("Alumno@TECSUP.EDU.PE"));
    }

    @Test
    void esDeTecsup_rechazaOtrosDominios() {
        assertFalse(validador.esDeTecsup("alumno@gmail.com"));
        assertFalse(validador.esDeTecsup("alumno@falso-tecsup.edu.pe"));
        assertFalse(validador.esDeTecsup("alumno@tecsup.edu.pe.evil.com"));
        assertFalse(validador.esDeTecsup("sin-arroba"));
        assertFalse(validador.esDeTecsup(null));
    }
}
