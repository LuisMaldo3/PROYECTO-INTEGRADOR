import assert from 'node:assert/strict'
import { test } from 'node:test'
import { ROLES, hayCambio } from './roles.js'

test('LISTA: ofrece los tres roles en orden', () => {
  assert.deepEqual(ROLES.map((r) => r.valor), ['usuario', 'organizador', 'administrador'])
  assert.ok(ROLES.every((r) => r.etiqueta.length > 0))
})

test('CAMBIO: solo si el rol elegido es válido y distinto del actual', () => {
  assert.equal(hayCambio('usuario', 'organizador'), true)
  assert.equal(hayCambio('usuario', 'usuario'), false)
  assert.equal(hayCambio('Administrador', 'administrador'), false)
  assert.equal(hayCambio('usuario', ''), false)
  assert.equal(hayCambio('usuario', 'superadmin'), false)
  assert.equal(hayCambio(undefined, 'organizador'), true)
})
