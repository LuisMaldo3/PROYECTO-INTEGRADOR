import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarCuerpo } from './actividades.js'
import { validarActividad } from './validacionActividad.js'

test('asignación: se envía organizadorId limpio al backend', () => {
  assert.equal(armarCuerpo({ organizadorId: '  abc123  ' }).organizadorId, 'abc123')
})
test('asignación: requiere seleccionar organizador', () => {
  assert.match(validarActividad({}).organizadorId, /organizador responsable/)
})
test('asignación: con organizador no hay error de organizador', () => {
  assert.equal(validarActividad({ organizadorId: 'abc123' }).organizadorId, undefined)
})
