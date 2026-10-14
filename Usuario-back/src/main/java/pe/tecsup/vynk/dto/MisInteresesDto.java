package pe.tecsup.vynk.dto;

import java.util.List;

/** Lo que eligió la persona. actualizadoEn es la fecha del último guardado (ISO) o null si nunca guardó. */
public record MisInteresesDto(List<String> intereses, String actualizadoEn) {
}
