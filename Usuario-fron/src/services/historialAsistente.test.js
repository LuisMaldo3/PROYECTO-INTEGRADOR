import assert from 'node:assert/strict'
import { test } from 'node:test'
import { claveHistorial, leerHistorial, guardarHistorial, borrarHistorial } from './historialAsistente.js'

test('Chat: historial aislado por cuenta, sin credenciales', () => {
  const datos = new Map()
  globalThis.localStorage = {
    getItem: k => datos.get(k) ?? null,
    setItem: (k, v) => datos.set(k, v),
    removeItem: k => datos.delete(k),
  }
  assert.notEqual(claveHistorial('ana@tecsup.edu.pe'), claveHistorial('juan@tecsup.edu.pe'))
  guardarHistorial('ana@tecsup.edu.pe', [{ rol: 'usuario', contenido: '¿Qué hay hoy?', token: 'no-guardar' }])
  assert.equal(leerHistorial('juan@tecsup.edu.pe').length, 0)
  assert.equal(leerHistorial('ana@tecsup.edu.pe')[0].contenido, '¿Qué hay hoy?')
  assert.equal(JSON.stringify(leerHistorial('ana@tecsup.edu.pe')).includes('no-guardar'), false)
  borrarHistorial('ana@tecsup.edu.pe')
  assert.deepEqual(leerHistorial('ana@tecsup.edu.pe'), [])
  delete globalThis.localStorage
})
