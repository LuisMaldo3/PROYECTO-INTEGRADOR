/** Revisiones del formulario «Nueva actividad» (sin React ni red, para probarlas con `npm test`). */

// Mismos límites y mensajes que Admin-back (actividades/servicio.py): si cambian allá, cambian aquí.
export const LIMITES = { nombreMin: 3, nombreMax: 80, descripcionMin: 10, descripcionMax: 500 }

const texto = (valor) => (typeof valor === 'string' ? valor.trim() : '')

const FORMATO_FECHA = /^\d{4}-\d{2}-\d{2}$/
const FORMATO_HORA = /^([01]\d|2[0-3]):[0-5]\d$/

/** true si es una fecha real "AAAA-MM-DD" (rechaza 2026-02-30). */
export function fechaValida(valor) {
  if (!FORMATO_FECHA.test(valor)) return false
  const [anio, mes, dia] = valor.split('-').map(Number)
  const fecha = new Date(Date.UTC(anio, mes - 1, dia))
  return fecha.getUTCFullYear() === anio && fecha.getUTCMonth() === mes - 1 && fecha.getUTCDate() === dia
}

/** true si es una hora "HH:MM" de 24 horas. */
export const horaValida = (valor) => FORMATO_HORA.test(valor)

/** Devuelve { campo: mensaje } solo con los campos que tienen error; {} si todo está bien. */
export function validarActividad({ nombre, descripcion, categoriaId, fecha, horaInicio, horaFin } = {}) {
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

  // Tarea 3: fecha y horario (mismos mensajes que Admin-back).
  const f = texto(fecha)
  const inicio = texto(horaInicio)
  const fin = texto(horaFin)

  if (!f) errores.fecha = 'Elige la fecha de la actividad.'
  else if (!fechaValida(f)) errores.fecha = 'La fecha no es válida.'

  if (!inicio) errores.horaInicio = 'Elige la hora de inicio.'
  else if (!horaValida(inicio)) errores.horaInicio = 'La hora de inicio no es válida.'

  if (!fin) errores.horaFin = 'Elige la hora de fin.'
  else if (!horaValida(fin)) errores.horaFin = 'La hora de fin no es válida.'
  // Con las dos horas bien escritas, "HH:MM" se compara como texto: "09:30" < "14:00".
  else if (!errores.horaInicio && fin <= inicio) errores.horaFin = 'La hora de fin debe ser después de la hora de inicio.'

  return errores
}
