/** Solo el backend puede certificar que una recomendación cabe en los huecos confirmados. */
export function cabeEnHorario(actividad) {
  return actividad?.cabeEnHorario === true &&
    /^\d{2}:\d{2}$/.test(actividad?.huecoInicio || '') &&
    /^\d{2}:\d{2}$/.test(actividad?.huecoFin || '')
}

export function descripcionCompatibilidad(actividad) {
  if (!cabeEnHorario(actividad)) return ''
  return `Espacio libre: ${actividad.huecoInicio} – ${actividad.huecoFin}`
}
