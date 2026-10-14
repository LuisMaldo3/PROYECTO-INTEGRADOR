package pe.tecsup.vynk.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.tecsup.vynk.service.CredencialInvalidaException;
import pe.tecsup.vynk.service.DominioNoPermitidoException;
import pe.tecsup.vynk.service.LoginIncorrectoException;
import pe.tecsup.vynk.service.SeleccionInvalidaException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CredencialInvalidaException.class)
    public ResponseEntity<Map<String, String>> credencialInvalida() {
        return ResponseEntity.status(401).body(Map.of(
                "codigo", "CREDENCIAL_INVALIDA",
                "mensaje", "No se pudo verificar tu cuenta"));
    }

    @ExceptionHandler(DominioNoPermitidoException.class)
    public ResponseEntity<Map<String, String>> dominioNoPermitido() {
        return ResponseEntity.status(403).body(Map.of(
                "codigo", "DOMINIO_NO_PERMITIDO",
                "mensaje", "Usa tu correo @tecsup.edu.pe"));
    }

    @ExceptionHandler(LoginIncorrectoException.class)
    public ResponseEntity<Map<String, String>> loginIncorrecto() {
        return ResponseEntity.status(401).body(Map.of(
                "codigo", "LOGIN_INCORRECTO",
                "mensaje", "Correo o contraseña incorrectos. Si nunca creaste una contraseña, entra con Google."));
    }

    /** La lista de intereses no se puede guardar (vacía, repetida o con uno que no existe). */
    @ExceptionHandler(SeleccionInvalidaException.class)
    public ResponseEntity<Map<String, String>> seleccionInvalida(SeleccionInvalidaException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "codigo", "SELECCION_INVALIDA",
                "mensaje", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        String campo = error != null ? error.getField() : "";
        String mensaje = switch (campo) {
            case "password" -> "La contraseña debe tener entre 8 y 64 caracteres.";
            case "correo" -> "Escribe un correo válido.";
            default -> "Revisa los datos enviados.";
        };
        return ResponseEntity.badRequest().body(Map.of(
                "codigo", "DATOS_INVALIDOS",
                "mensaje", mensaje));
    }

    /** MongoDB caido o inalcanzable: responde claro en vez de un 500 generico. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> baseDeDatos(DataAccessException ex) {
        return ResponseEntity.status(503).body(Map.of(
                "codigo", "BASE_DE_DATOS_NO_DISPONIBLE",
                "mensaje", "El servidor no pudo conectarse a la base de datos. Intenta de nuevo en unos minutos."));
    }
}
