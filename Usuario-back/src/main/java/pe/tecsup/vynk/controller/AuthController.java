package pe.tecsup.vynk.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tecsup.vynk.dto.LoginRequest;
import pe.tecsup.vynk.dto.LoginResponse;
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
}