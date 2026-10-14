package pe.tecsup.vynk.dto;

import java.util.List;

/** Cuerpo de PUT /api/intereses/mios: { "intereses": ["tecnologia", "musica"] } */
public record InteresesRequest(List<String> intereses) {
}
