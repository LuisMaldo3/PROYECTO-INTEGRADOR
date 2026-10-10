package pe.tecsup.vynk.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.dto.LoginPasswordRequest;
import pe.tecsup.vynk.dto.LoginRequest;
import pe.tecsup.vynk.dto.LoginResponse;
import pe.tecsup.vynk.dto.PasswordRequest;
import pe.tecsup.vynk.dto.UsuarioDto;
import pe.tecsup.vynk.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.idToken());
    }

    @PostMapping("/login-password")
    public LoginResponse loginPassword(@Valid @RequestBody LoginPasswordRequest request) {
        return auth.loginConPassword(request.correo(), request.password());
    }

    /** «Quién soy»: devuelve el usuario del token con su rol actual. Requiere Authorization: Bearer ... */
    @GetMapping("/me")
    public UsuarioDto quienSoy(@AuthenticationPrincipal Jwt jwt) {
        return auth.quienSoy(jwt.getSubject());
    }

    /** Cierra la sesión: el token usado deja de servir. Requiere Authorization: Bearer ... */
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(@AuthenticationPrincipal Jwt jwt) {
        auth.cerrarSesion(jwt);
        return ResponseEntity.noContent().build();
    }

    /** Requiere el JWT de VYNK (Authorization: Bearer ...). */
    @PutMapping("/password")
    public ResponseEntity<Void> definirPassword(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody PasswordRequest request) {
        auth.definirPassword(jwt.getSubject(), request.password());
        return ResponseEntity.noContent().build();
    }
}
