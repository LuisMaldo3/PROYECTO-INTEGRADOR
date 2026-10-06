package pe.tecsup.vynk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * US-02 (Mishel): leer el correo del intento de login y comprobar
 * que pertenece al dominio institucional de Tecsup.
 */
@Component
public class ValidadorCorreoInstitucional {

    private final String dominio;

    public ValidadorCorreoInstitucional(@Value("${vynk.auth.dominio}") String dominio) {
        this.dominio = dominio == null ? "" : dominio.trim().toLowerCase();
    }

    /** Lee y normaliza el correo de quien inicia sesión. */
    public String leerCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new CredencialInvalidaException();
        }
        return correo.trim().toLowerCase();
    }

    /** Revisa que el correo sea del dominio institucional (@tecsup.edu.pe). */
    public boolean esDeTecsup(String correo) {
        if (correo == null || correo.isBlank() || dominio.isBlank()) {
            return false;
        }
        String normalizado = correo.trim().toLowerCase();
        int at = normalizado.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        String dominioCorreo = normalizado.substring(at);
        return dominioCorreo.equals(dominio);
    }
}
