package pe.tecsup.vynk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.tecsup.vynk.service.CredencialInvalidaException;
import pe.tecsup.vynk.service.DominioNoPermitidoException;

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
}