/** Revisiones del formulario «Nueva actividad» (sin React ni red, para probarlas con `npm test`). */

// Mismos límites y mensajes que Admin-back (actividades/servicio.py): si cambian allá, cambian aquí.
export const LIMITES = { nombreMin: 3, nombreMax: 80, descripcionMin: 10, descripcionMax: 500 }

const texto = (valor) => (typeof valor === 'string' ? valor.trim() : '')

/** Devuelve { campo: mensaje } solo con los campos que tienen error; {} si todo está bien. */
export function validarActividad({ nombre, descripcion, categoriaId } = {}) {
  const errores = {}
  const n = texto(nombre)
  const d = texto(descripcion)

  if (!n) errores.nombre = 'Escribe el nombre de la actividad.'
  else if (n.length < LIMITES.nombreMin) errores.nombre = `El nombre debe tener al menos ${LIMITES.nombreMin} caracteres.`
  else if (n.length > LIMITES.nombreMax) errores.nombre = `El nombre no puede pasar de ${LIMITES.nombreMax} caracteres.`

  if (!d) errores.descripcion = 'Escribe una descripción.'
  else if (d.length < LIMITES.descripcionMin)
    errores.descripcion = `La descripción debe tener al menos ${LIMITES.descripcionMin} caracteres.`
  else if (d.length > LIMITES.descripcionMax)
    errores.descripcion = `La descripción no puede pasar de ${LIMITES.descripcionMax} caracteres.`

  if (!texto(categoriaId)) errores.categoriaId = 'Elige una categoría.'

  return errores
}
