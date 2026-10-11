import assert from 'node:assert/strict'
import { test } from 'node:test'
import { listaDe } from './listas.js'

test('LISTA: devuelve la lista de la clave pedida', () => {
  const ubicaciones = [{ id: 'auditorio', nombre: 'Auditorio' }]
  assert.deepEqual(listaDe({ ubicaciones }, 'ubicaciones'), ubicaciones)
  assert.deepEqual(listaDe({ ubicaciones: [] }, 'ubicaciones'), [])
})

test('LISTA: si falta, es null o no es una lista, devuelve [] (no rompe el desplegable)', () => {
  for (const mala of [undefined, null, {}, { ubicaciones: null }, { ubicaciones: 'auditorio' }, { ubicaciones: { a: 1 } }, [], 'texto', 5]) {
    assert.deepEqual(listaDe(mala, 'ubicaciones'), [], JSON.stringify(mala))
  }
})

test('LISTA: no confunde una clave con otra', () => {
  assert.deepEqual(listaDe({ categorias: [{ id: 'x' }] }, 'ubicaciones'), [])
})
