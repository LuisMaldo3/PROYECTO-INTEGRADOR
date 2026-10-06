package pe.tecsup.vynk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginPasswordRequest(@NotBlank @Email String correo, @NotBlank String password) {
}
