/** Convierte una actividad del API a los valores editables del formulario. */
export function datosEdicionActividad(a) {
  return {
    nombre: a.nombre ?? '', descripcion: a.descripcion ?? '',
    categoriaId: a.categoria?.id ?? a.categoriaId ?? '',
    ubicacionId: a.ubicacion?.id ?? a.ubicacionId ?? '',
    organizadorId: a.organizadorId ?? a.organizador?.id ?? '',
    fecha: a.fecha ?? '', horaInicio: a.horaInicio ?? '', horaFin: a.horaFin ?? '',
    maximoParticipantes: a.maximoParticipantes == null ? '' : String(a.maximoParticipantes),
  }
}
