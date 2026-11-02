import test from 'node:test'
import assert from 'node:assert/strict'
import { ganadoresOficiales } from './detalleCompetenciaService.js'

test('US-22 T08: solo identifica ganadores publicados (puesto 1)', () => {
  assert.deepEqual(ganadoresOficiales({posiciones:[{nombre:'Equipo A',puesto:1,puntos:30,areaTecsup:'Tecnología Digital'},{nombre:'Equipo B',puesto:2,puntos:10}]}), [{nombre:'Equipo A',puntos:30,areaTecsup:'Tecnología Digital'}])
  assert.deepEqual(ganadoresOficiales(null), [])
})
