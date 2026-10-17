import test from 'node:test'
import assert from 'node:assert/strict'
import { validarAviso } from './avisos.js'

test('programa avisos futuros en UTC correctamente', () => {
  const ahora = new Date('2026-10-26T18:00:00Z')
  const v = validarAviso({ titulo: ' Aviso ', mensaje: ' Comunicado ', programar: true, fechaLocal: '2026-10-27T21:30' }, ahora)
  assert.equal(v.titulo, 'Aviso')
  assert.equal(v.mensaje, 'Comunicado')
  assert.equal(Object.keys(v.errores).length, 0)
  assert.ok(v.publicarEn.endsWith('Z'))
})

test('no acepta programar fechas pasadas', () => {
  const v = validarAviso({ titulo: 'Aviso', mensaje: 'Comunicado', programar: true, fechaLocal: '2025-01-01T12:00' })
  assert.ok(v.errores.publicarEn)
})

test('sin programación publica inmediatamente', () => {
  const v = validarAviso({ titulo: 'Aviso', mensaje: 'Comunicado', programar: false })
  assert.equal(v.publicarEn, null)
})
