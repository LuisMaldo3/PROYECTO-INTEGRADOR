import assert from 'node:assert/strict'
import { test } from 'node:test'
import { formatearFecha, resumen } from './formatoActividad.js'

test('RESUMEN: el texto corto queda igual y el largo se recorta con …', () => {
  assert.equal(resumen('  Hola  '), 'Hola')
  assert.equal(resumen('a'.repeat(80)), 'a'.repeat(80))
  const largo = resumen('a'.repeat(200), 20)
  assert.equal(largo.length, 20)
  assert.ok(largo.endsWith('…'))
  assert.equal(resumen(undefined), '')
})

test('FECHA: una fecha válida muestra el año; lo demás es —', () => {
  assert.match(formatearFecha('2026-10-14T15:30:00+00:00'), /2026/)
  assert.equal(formatearFecha(''), '—')
  assert.equal(formatearFecha(null), '—')
  assert.equal(formatearFecha('no es fecha'), '—')
})
