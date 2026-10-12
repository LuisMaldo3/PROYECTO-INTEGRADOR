/** Revisiones del formulario «Nueva actividad»: datos, categoría, ubicación, fecha y horario (sin React ni red, para probarlas con `npm test`). */

// Mismos límites y mensajes que Admin-back (actividades/servicio.py): si cambian allá, cambian aquí.
export const LIMITES = { nombreMin: 3, nombreMax: 80, descripcionMin: 10, descripcionMax: 500 }

const texto = (valor) => (typeof valor === 'string' ? valor.trim() : '')

const FORMATO_FECHA = /^(\d{4})-(\d{2})-(\d{2})$/
const FORMATO_HORA = /^([01]\d|2[0-3]):[0-5]\d$/ // "HH:MM" en 24 h, como entrega <input type="time">

/** true si "AAAA-MM-DD" es un día real del calendario (rechaza 2026-02-30). */
function esFechaReal(valor) {
  const partes = FORMATO_FECHA.exec(valor)
  if (!partes) return false
  const [anio, mes, dia] = partes.slice(1).map(Number)
  const fecha = new Date(anio, mes - 1, dia)
  return fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia
}

/** Devuelve { campo: mensaje } solo con los campos que tienen error; {} si todo está bien. */
export function validarActividad({ nombre, descripcion, categoriaId, ubicacionId, organizadorId, fecha, horaInicio, horaFin, maximoParticipantes } = {}) {
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
  if (!texto(ubicacionId)) errores.ubicacionId = 'Elige una ubicación.'
  if (!texto(organizadorId)) errores.organizadorId = 'Elige un organizador responsable.'

  if (!/^[1-9]\d*$/.test(String(maximoParticipantes ?? "")) || !Number.isSafeInteger(Number(maximoParticipantes))) {
    errores.maximoParticipantes = "El máximo de participantes debe ser un entero mayor que cero."
  }

  // Fecha y horario (mismos mensajes que Admin-back: actividades/horario.py)
  const f = texto(fecha)
  const inicio = texto(horaInicio)
  const fin = texto(horaFin)

  if (!f) errores.fecha = 'Elige la fecha de la actividad.'
  else if (!esFechaReal(f)) errores.fecha = 'La fecha no es válida.'

  if (!inicio) errores.horaInicio = 'Elige la hora de inicio.'
  else if (!FORMATO_HORA.test(inicio)) errores.horaInicio = 'La hora de inicio no es válida.'

  // La hora de fin es opcional; si se escribe, debe ser después de la de inicio ("HH:MM" se compara bien como texto).
  if (fin) {
    if (!FORMATO_HORA.test(fin)) errores.horaFin = 'La hora de fin no es válida.'
    else if (!errores.horaInicio && fin <= inicio) errores.horaFin = 'La hora de fin debe ser después de la de inicio.'
  }

  return errores
}
