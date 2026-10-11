/** Lógica pura de actividades en la web (sin React ni red, para probarla con `npm test`). */

/** Lo que se envía a Admin-back: solo los tres campos, sin espacios sobrantes. */
export function armarCuerpo({ nombre, descripcion, categoriaId } = {}) {
  const limpio = (valor) => (typeof valor === 'string' ? valor.trim() : '')
  return { nombre: limpio(nombre), descripcion: limpio(descripcion), categoriaId: limpio(categoriaId) }
}
