/** Lógica pura de actividades en la web (sin React ni red, para probarla con `npm test`). */

/** Lo que se envía a Admin-back: solo estos campos, sin espacios sobrantes. La hora de fin vacía se envía como "". */
export function armarCuerpo({ nombre, descripcion, categoriaId, fecha, horaInicio, horaFin } = {}) {
  const limpio = (valor) => (typeof valor === 'string' ? valor.trim() : '')
  return {
    nombre: limpio(nombre),
    descripcion: limpio(descripcion),
    categoriaId: limpio(categoriaId),
    fecha: limpio(fecha),
    horaInicio: limpio(horaInicio),
    horaFin: limpio(horaFin),
  }
}
