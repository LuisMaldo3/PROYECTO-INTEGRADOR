/** Convierte una actividad del API a los valores editables del formulario. */
export function datosEdicionActividad(a) {
  return {
    nombre: a.nombre ?? '', descripcion: a.descripcion ?? '',
    categoriaId: a.categoria?.id ?? a.categoriaId ?? '',
    ubicacionId: a.ubicacion?.id ?? a.ubicacionId ?? '',
    organizadorId: a.organizadorId ?? a.organizador?.id ?? '',
    fecha: a.fecha ?? '', horaInicio: a.horaInicio ?? '', horaFin: a.horaFin ?? '',
    maximoParticipantes: a.maximoParticipantes == null ? '' : String(a.maximoParticipantes),
    tipoActividad: a.tipoActividad || "actividad", modalidad: a.modalidad || "individual",
    maxIntegrantesEquipo: a.maxIntegrantesEquipo == null ? "" : String(a.maxIntegrantesEquipo),
    areaTecsup: a.areaTecsup || "", encargadosPorArea: Object.fromEntries(Object.entries(a.encargadosPorArea || {}).map(([k,v]) => [k, v?.id || v])),
    reglasCompetencia: a.reglasCompetencia || "", puntosPorPuesto: (a.puntosPorPuesto || []).join(", "), portada: a.portada || null,
  }
}
