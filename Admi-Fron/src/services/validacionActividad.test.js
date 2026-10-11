import assert from 'node:assert/strict'
import { test } from 'node:test'
import { validarActividad } from './validacionActividad.js'

const OK = { nombre: 'Torneo de fútbol', descripcion: 'Campeonato abierto para todos.', categoriaId: 'deportes' }

test('REVISIÓN: datos correctos no dan errores', () => {
  assert.deepEqual(validarActividad(OK), {})
})

test('REVISIÓN: todo vacío marca los tres campos', () => {
  assert.deepEqual(Object.keys(validarActividad({})).sort(), ['categoriaId', 'descripcion', 'nombre'])
  assert.deepEqual(Object.keys(validarActividad()).sort(), ['categoriaId', 'descripcion', 'nombre'])
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
  assert.equal(Object.keys(validarActividad({ nombre: 5, descripcion: ['x'], categoriaId: null })).length, 3)
})
