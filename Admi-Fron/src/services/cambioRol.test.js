import assert from 'node:assert/strict'
import { test } from 'node:test'
import { ESTADO_INICIAL, reducir } from './cambioRol.js'

test('FLUJO: elegir un rol abre la confirmación', () => {
  assert.deepEqual(reducir(ESTADO_INICIAL, { tipo: 'pedir', rol: 'organizador' }), {
    pendiente: 'organizador',
    enviando: false,
    error: '',
  })
})

test('FLUJO: cancelar vuelve al inicio', () => {
  const abierto = reducir(ESTADO_INICIAL, { tipo: 'pedir', rol: 'organizador' })
  assert.deepEqual(reducir(abierto, { tipo: 'cancelar' }), ESTADO_INICIAL)
})

test('FLUJO: mientras se guarda no se puede cancelar ni elegir otro rol', () => {
  const guardando = reducir(reducir(ESTADO_INICIAL, { tipo: 'pedir', rol: 'organizador' }), { tipo: 'enviando' })
  assert.equal(guardando.enviando, true)
  assert.equal(reducir(guardando, { tipo: 'cancelar' }), guardando)
  assert.equal(reducir(guardando, { tipo: 'pedir', rol: 'administrador' }), guardando)
})

test('FLUJO: si falla, queda abierto con el error y se puede reintentar', () => {
  const guardando = reducir(reducir(ESTADO_INICIAL, { tipo: 'pedir', rol: 'organizador' }), { tipo: 'enviando' })
  const fallo = reducir(guardando, { tipo: 'fallo', mensaje: 'Rol no válido.' })
  assert.deepEqual(fallo, { pendiente: 'organizador', enviando: false, error: 'Rol no válido.' })
  assert.equal(reducir(fallo, { tipo: 'enviando' }).error, '')
})

test('FLUJO: al terminar bien se limpia todo', () => {
  const guardando = reducir(reducir(ESTADO_INICIAL, { tipo: 'pedir', rol: 'organizador' }), { tipo: 'enviando' })
  assert.deepEqual(reducir(guardando, { tipo: 'listo' }), ESTADO_INICIAL)
})

test('FLUJO: guardar sin haber elegido nada no hace nada', () => {
  assert.equal(reducir(ESTADO_INICIAL, { tipo: 'enviando' }), ESTADO_INICIAL)
})
