package pe.tecsup.vynk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.tecsup.vynk.service.CredencialInvalidaException;
import pe.tecsup.vynk.service.DominioNoPermitidoException;
import pe.tecsup.vynk.service.LoginIncorrectoException;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos() {
        return ResponseEntity.badRequest().body(Map.of(
                "codigo", "DATOS_INVALIDOS",
                "mensaje", "Revisa los datos enviados (la contraseña debe tener entre 8 y 64 caracteres)."));
    }
}
