import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerInscritos } from './organizadorService.js'

test('filtra inscritos por correo y envía Bearer sin modificar la ruta', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/inscritos\?buscar=Mar%C3%ADa%40tecsup\.edu\.pe$/)
    assert.equal(opciones.headers.Authorization, 'Bearer token')
    return { ok: true, json: async () => ({ actividad: { nombre: 'Feria' }, inscritos: [{ nombre: 'María' }], totalInscritos: 12 }) }
  }
  const respuesta = await obtenerInscritos('token', 'act1', undefined, '  María@tecsup.edu.pe ')
  assert.equal(respuesta.inscritos.length, 1)
  assert.equal(respuesta.totalInscritos, 12)
})

test('evita enviar búsquedas de más de 100 caracteres', async () => {
  await assert.rejects(obtenerInscritos('token', 'act1', undefined, 'A'.repeat(101)), /100 caracteres/)
})
