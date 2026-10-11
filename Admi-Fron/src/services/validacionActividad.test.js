import assert from 'node:assert/strict'
import { test } from 'node:test'
import { fechaValida, horaValida, validarActividad } from './validacionActividad.js'

const OK = {
  nombre: 'Torneo de fútbol',
  descripcion: 'Campeonato abierto para todos.',
  categoriaId: 'deportes',
  fecha: '2026-10-19',
  horaInicio: '11:00',
  horaFin: '14:30',
}
const TODOS = ['categoriaId', 'descripcion', 'fecha', 'horaFin', 'horaInicio', 'nombre']

test('REVISIÓN: datos correctos no dan errores', () => {
  assert.deepEqual(validarActividad(OK), {})
})

test('REVISIÓN: todo vacío marca todos los campos', () => {
  assert.deepEqual(Object.keys(validarActividad({})).sort(), TODOS)
  assert.deepEqual(Object.keys(validarActividad()).sort(), TODOS)
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
  assert.equal(Object.keys(validarActividad({ nombre: 5, descripcion: ['x'], categoriaId: null })).length, 6)
})

test('FECHA: obligatoria, con formato AAAA-MM-DD y que exista de verdad', () => {
  assert.equal(validarActividad({ ...OK, fecha: '' }).fecha, 'Elige la fecha de la actividad.')
  assert.equal(validarActividad({ ...OK, fecha: '   ' }).fecha, 'Elige la fecha de la actividad.')
  for (const mala of ['19/10/2026', '2026-13-01', '2026-02-30', '2026-1-5', 'mañana']) {
    assert.equal(validarActividad({ ...OK, fecha: mala }).fecha, 'La fecha no es válida.', mala)
  }
  assert.equal(validarActividad({ ...OK, fecha: '2028-02-29' }).fecha, undefined)
  assert.equal(validarActividad({ ...OK, fecha: '2026-02-29' }).fecha, 'La fecha no es válida.')
})

test('FECHA y HORA: funciones de apoyo', () => {
  assert.equal(fechaValida('2026-10-19'), true)
  assert.equal(fechaValida('2026-10-32'), false)
  assert.equal(horaValida('00:00'), true)
  assert.equal(horaValida('23:59'), true)
  assert.equal(horaValida('24:00'), false)
  assert.equal(horaValida('9:00'), false)
  assert.equal(horaValida('11:60'), false)
})

test('HORARIO: las dos horas son obligatorias y con formato HH:MM', () => {
  assert.equal(validarActividad({ ...OK, horaInicio: '' }).horaInicio, 'Elige la hora de inicio.')
  assert.equal(validarActividad({ ...OK, horaFin: '' }).horaFin, 'Elige la hora de fin.')
  assert.equal(validarActividad({ ...OK, horaInicio: '25:00' }).horaInicio, 'La hora de inicio no es válida.')
  assert.equal(validarActividad({ ...OK, horaFin: '11:60' }).horaFin, 'La hora de fin no es válida.')
})

test('HORARIO: la hora de fin debe ser después de la de inicio', () => {
  const mensaje = 'La hora de fin debe ser después de la hora de inicio.'
  assert.equal(validarActividad({ ...OK, horaInicio: '14:00', horaFin: '11:00' }).horaFin, mensaje)
  assert.equal(validarActividad({ ...OK, horaInicio: '11:00', horaFin: '11:00' }).horaFin, mensaje)
  assert.equal(validarActividad({ ...OK, horaInicio: '11:00', horaFin: '11:01' }).horaFin, undefined)
  assert.equal(validarActividad({ ...OK, horaInicio: '00:00', horaFin: '23:59' }).horaFin, undefined)
})

test('HORARIO: no compara las horas si la de inicio está mal escrita', () => {
  const errores = validarActividad({ ...OK, horaInicio: '25:00', horaFin: '10:00' })
  assert.equal(errores.horaInicio, 'La hora de inicio no es válida.')
  assert.equal(errores.horaFin, undefined)
})
