package pe.tecsup.vynk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequest(@NotBlank @Size(min = 8, max = 64) String password) {
}
