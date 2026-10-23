import test from 'node:test'
import assert from 'node:assert/strict'
import { cabeEnHorario, descripcionCompatibilidad } from './compatibilidadHorario.js'

test('muestra la etiqueta solo con certificación del backend y huecos horarios completos', () => {
  assert.equal(cabeEnHorario({ cabeEnHorario: true, huecoInicio: '11:00', huecoFin: '13:00' }), true)
  assert.equal(cabeEnHorario({ cabeEnHorario: false, huecoInicio: '11:00', huecoFin: '13:00' }), false)
  assert.equal(cabeEnHorario({ cabeEnHorario: true, huecoInicio: '', huecoFin: '13:00' }), false)
  assert.equal(cabeEnHorario({ horaInicio: '11:00', horaFin: '12:00' }), false)
  assert.equal(cabeEnHorario(null), false)
})

test('la franja que se presenta proviene de datos válidos', () => {
  assert.equal(descripcionCompatibilidad({ cabeEnHorario: true, huecoInicio: '12:00', huecoFin: '14:00' }), 'Espacio libre: 12:00 – 14:00')
  assert.equal(descripcionCompatibilidad({ cabeEnHorario: false, huecoInicio: '12:00', huecoFin: '14:00' }), '')
})
