/** Cambios que requieren preguntar si se notifica a los inscritos. */
export function hayCambioNotificable(actividad, valores) {
  if (!actividad || !valores) return false
  const cambianHoras = ['fecha', 'horaInicio', 'horaFin'].some(
    (clave) => (actividad[clave] ?? '') !== (valores[clave] ?? ''),
  )
  return cambianHoras || (actividad.ubicacion?.id ?? '') !== (valores.ubicacionId ?? '')
}
