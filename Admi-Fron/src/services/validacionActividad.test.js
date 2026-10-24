import assert from 'node:assert/strict'
import { test } from 'node:test'
import { validarActividad } from './validacionActividad.js'

const OK = {
  nombre: 'Torneo de fútbol',
  descripcion: 'Campeonato abierto para todos.',
  categoriaId: 'deportes',
  ubicacionId: 'auditorio',
  organizadorId: 'organizador-1',
  maximoParticipantes: '30',
  fecha: '2026-10-19',
  horaInicio: '11:00',
  horaFin: '14:30',
}

test('REVISIÓN: datos correctos no dan errores', () => {
  assert.deepEqual(validarActividad(OK), {})
})

test('REVISIÓN: todo vacío marca los campos obligatorios (la hora de fin no lo es)', () => {
  const obligatorios = ['categoriaId', 'descripcion', 'fecha', 'horaInicio', 'maximoParticipantes', 'nombre', 'organizadorId', 'ubicacionId']
  assert.deepEqual(Object.keys(validarActividad({})).sort(), obligatorios)
  assert.deepEqual(Object.keys(validarActividad()).sort(), obligatorios)
})

test('REVISIÓN: solo espacios cuenta como vacío', () => {
  assert.equal(validarActividad({ ...OK, nombre: '     ' }).nombre, 'Escribe el nombre de la actividad.')
  assert.equal(validarActividad({ ...OK, descripcion: '   ' }).descripcion, 'Escribe una descripción.')
  assert.equal(validarActividad({ ...OK, categoriaId: '  ' }).categoriaId, 'Elige una categoría.')
})

test('REVISIÓN: límites del nombre (3 a 80, sin contar espacios de los bordes)', () => {
  assert.match(validarActividad({ ...OK, nombre: 'ab' }).nombre, /al menos 3/)
  assert.match(validarActividad({ ...OK, nombre: ' ab ' }).nombre, /al menos 3/)
  assert.equal(validarActividad({ ...OK, nombre: 'abc' }).nombre, undefined)
  assert.equal(validarActividad({ ...OK, nombre: 'n'.repeat(80) }).nombre, undefined)
  assert.match(validarActividad({ ...OK, nombre: 'n'.repeat(81) }).nombre, /pasar de 80/)
})

test('REVISIÓN: límites de la descripción (10 a 500)', () => {
  assert.match(validarActividad({ ...OK, descripcion: 'corta' }).descripcion, /al menos 10/)
  assert.equal(validarActividad({ ...OK, descripcion: 'x'.repeat(10) }).descripcion, undefined)
  assert.equal(validarActividad({ ...OK, descripcion: 'x'.repeat(500) }).descripcion, undefined)
  assert.match(validarActividad({ ...OK, descripcion: 'x'.repeat(501) }).descripcion, /pasar de 500/)
})

test('REVISIÓN: valores que no son texto no rompen nada', () => {
  assert.equal(Object.keys(validarActividad({ nombre: 5, descripcion: ['x'], categoriaId: null, ubicacionId: 3, fecha: 7, horaInicio: {}, horaFin: [] })).length, 8)
})

test('FECHA: obligatoria y un día real del calendario', () => {
  assert.equal(validarActividad({ ...OK, fecha: '' }).fecha, 'Elige la fecha de la actividad.')
  for (const mala of ['2026-02-30', '2026-13-01', '2026-00-10', '19/10/2026', '2026-1-5', 'mañana']) {
    assert.equal(validarActividad({ ...OK, fecha: mala }).fecha, 'La fecha no es válida.', mala)
  }
  assert.equal(validarActividad({ ...OK, fecha: '2028-02-29' }).fecha, undefined) // año bisiesto
  assert.equal(validarActividad({ ...OK, fecha: '2026-02-29' }).fecha, 'La fecha no es válida.') // no bisiesto
})

test('HORA DE INICIO: obligatoria y en formato HH:MM de 24 horas', () => {
  assert.equal(validarActividad({ ...OK, horaInicio: '' }).horaInicio, 'Elige la hora de inicio.')
  for (const mala of ['25:00', '9:00', '10:60', 'abc', '11:00 a. m.']) {
    assert.equal(validarActividad({ ...OK, horaInicio: mala, horaFin: '' }).horaInicio, 'La hora de inicio no es válida.', mala)
  }
  for (const buena of ['00:00', '09:05', '23:59']) {
    assert.equal(validarActividad({ ...OK, horaInicio: buena, horaFin: '' }).horaInicio, undefined, buena)
  }
})

test('HORA DE FIN: es opcional', () => {
  assert.equal(validarActividad({ ...OK, horaFin: '' }).horaFin, undefined)
  assert.equal(validarActividad({ ...OK, horaFin: '   ' }).horaFin, undefined)
  assert.equal(validarActividad({ fecha: OK.fecha, horaInicio: '15:30', nombre: OK.nombre, descripcion: OK.descripcion, categoriaId: 'x' }).horaFin, undefined)
})

test('HORA DE FIN: debe ser después de la de inicio', () => {
  const dice = 'La hora de fin debe ser después de la de inicio.'
  assert.equal(validarActividad({ ...OK, horaInicio: '14:30', horaFin: '14:30' }).horaFin, dice) // igual
  assert.equal(validarActividad({ ...OK, horaInicio: '14:30', horaFin: '11:00' }).horaFin, dice) // antes
  assert.equal(validarActividad({ ...OK, horaInicio: '14:30', horaFin: '14:31' }).horaFin, undefined) // un minuto después
  assert.equal(validarActividad({ ...OK, horaInicio: '09:00', horaFin: '10:00' }).horaFin, undefined) // el 09:00 < 10:00 también como texto
})

test('HORA DE FIN: con formato malo dice eso y no que es anterior; sin inicio válido no se compara', () => {
  assert.equal(validarActividad({ ...OK, horaFin: '99:99' }).horaFin, 'La hora de fin no es válida.')
  const sinInicio = validarActividad({ ...OK, horaInicio: '', horaFin: '10:00' })
  assert.equal(sinInicio.horaInicio, 'Elige la hora de inicio.')
  assert.equal(sinInicio.horaFin, undefined)
})

test('UBICACIÓN: es obligatoria; los espacios y lo que no es texto cuentan como vacío', () => {
  for (const vacia of ['', '   ', undefined, null, 5, ['auditorio']]) {
    assert.equal(validarActividad({ ...OK, ubicacionId: vacia }).ubicacionId, 'Elige una ubicación.', String(vacia))
  }
  assert.equal(validarActividad({ ...OK, ubicacionId: 'patio-central' }).ubicacionId, undefined)
})
