package pe.tecsup.vynk.dto;

import java.util.List;

/** Estado de personalización de un estudiante, común para web y Android. */
public record MisInteresesDto(List<String> intereses, String actualizadoEn, boolean configurados, boolean omitido) {
}
