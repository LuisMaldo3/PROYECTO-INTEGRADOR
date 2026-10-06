package pe.tecsup.vynk.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String idToken) {
}