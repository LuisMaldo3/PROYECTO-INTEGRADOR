import assert from 'node:assert/strict'
import { test } from 'node:test'
import { actividadesEnCruce } from './cruces.js'

test('CRUCES: identifica las dos actividades de cada cruce', () => {
  assert.deepEqual([...actividadesEnCruce([{ actividadAId: '1', actividadBId: '2' }])], ['1', '2'])
})

test('CRUCES: no resalta nada cuando no hay cruces', () => {
  assert.equal(actividadesEnCruce([]).size, 0)
})

test('CRUCES: no repite una actividad que está en varios cruces y ignora ids vacíos', () => {
  const ids = actividadesEnCruce([
    { actividadAId: '1', actividadBId: '2' },
    { actividadAId: '2', actividadBId: '3' },
    { actividadAId: null, actividadBId: '' },
  ])
  assert.deepEqual([...ids].sort(), ['1', '2', '3'])
})
