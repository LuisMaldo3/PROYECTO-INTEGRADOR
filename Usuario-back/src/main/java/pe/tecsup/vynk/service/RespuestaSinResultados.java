package pe.tecsup.vynk.service;

import java.util.List;

/** Respuesta contextual sin actividades ficticias, utilizada por el chat de VYNK. */
public final class RespuestaSinResultados {
    private RespuestaSinResultados() {}

    public static AsistenteHorariosService.Respuesta para(ConsultaTemaHorario consulta) {
        String criterio = "para " + consulta.tema();
        if (consulta.fecha() != null) criterio += " el " + consulta.fecha();
        if (consulta.hora() != null) {
            criterio += switch (consulta.modo()) {
                case DESPUES -> " después de las ";
                case ANTES -> " antes de las ";
                case EN, CUALQUIERA -> " a las ";
            };
            criterio += consulta.hora();
        }
        String texto = "No encontré actividades publicadas " + criterio
                + " con cupos disponibles. Puedes probar otro día, otra hora u otro tema.";
        if (consulta.tardeInferida()) texto += " Interpreté la hora consultada como "
                + consulta.hora() + " (tarde); puedes precisarla en tu pregunta.";
        return new AsistenteHorariosService.Respuesta(texto, "catalogo",
                "No hay coincidencias en el catálogo consultado; no se han inventado actividades.",
                List.of(), List.of(), "tema_horario", List.of());
    }
}
