import test from 'node:test'
import assert from 'node:assert/strict'
import { validarAviso } from './avisos.js'
test('rechaza titulo y mensaje vacios', () => {
  assert.deepEqual(Object.keys(validarAviso({ titulo: ' ', mensaje: '' }).errores), ['titulo', 'mensaje'])
})
test('acepta y limpia aviso valido', () => {
  const r = validarAviso({ titulo: ' Hola ', mensaje: ' Mundo ' })
  assert.equal(r.titulo, 'Hola')
  assert.equal(r.mensaje, 'Mundo')
  assert.deepEqual(r.errores, {})
})
test('rechaza textos demasiado largos', () => {
  const r = validarAviso({ titulo: 'a'.repeat(101), mensaje: 'a'.repeat(2001) })
  assert.ok(r.errores.titulo && r.errores.mensaje)
})
