import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizarReemplazos, diagnosticoCupo } from './reemplazos.js'

test('US-36: identifica exceso de cupos sin aumentar el límite', () => {
  const a = normalizarReemplazos({ actividades: [{ id: 'a', nombre: 'Taller', unidadCupo: 'estudiantes', capacidad: 2, ocupados: 3 }] }).actividades[0]
  assert.equal(a.capacidad, 2)
  assert.deepEqual(diagnosticoCupo(a), { estado: 'exceso', etiqueta: 'Excede en 1', disponibles: 0 })
})
test('US-36: distingue equipos de personas y ocupación', () => {
  const a = normalizarReemplazos({ actividades: [{ id: 'b', unidadCupo: 'equipos', capacidad: 2, ocupados: 1, personasInscritas: 5, equiposIncompletos: 1 }] }).actividades[0]
  assert.equal(a.ocupados, 1)
  assert.equal(a.personasInscritas, 5)
  assert.equal(diagnosticoCupo(a).disponibles, 1)
  assert.equal(a.equiposIncompletos, 1)
})
test('US-36: sin eventos auditados no inventa historial de reemplazos', () => {
  const data = normalizarReemplazos({ actividades: [{ id: 'x' }], historialDisponible: false })
  assert.deepEqual(data.actividades[0].historial, [])
  assert.equal(data.historialDisponible, false)
})
