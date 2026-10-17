import test from 'node:test'
import assert from 'node:assert/strict'
import { hayCambioNotificable } from './cambiosNotificables.js'

const actividad = { fecha: '2026-11-05', horaInicio: '10:00', horaFin: '12:00', ubicacion: { id: 'aula-1' } }
const valores = { fecha: '2026-11-05', horaInicio: '10:00', horaFin: '12:00', ubicacionId: 'aula-1' }

test('detecta ubicación cambiada sin cambios de horario', () => {
  assert.equal(hayCambioNotificable(actividad, { ...valores, ubicacionId: 'aula-2' }), true)
})
test('detecta cambios de fecha u hora sin cambio de ubicación', () => {
  assert.equal(hayCambioNotificable(actividad, { ...valores, horaFin: '13:00' }), true)
})
test('no solicita aviso cuando los campos comunicables son iguales', () => {
  assert.equal(hayCambioNotificable(actividad, valores), false)
})
