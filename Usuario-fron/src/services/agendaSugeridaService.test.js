import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerAgendaSugerida } from './agendaSugeridaService.js'

test('envía el token y procesa la agenda sugerida', async (t) => {
  const original = globalThis.fetch
  t.after(() => { globalThis.fetch = original })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/api\/agenda\/sugerida$/)
    assert.equal(opciones.headers.Authorization, 'Bearer token-estudiante')
    return { ok: true, json: async () => ({ origen: 'ia', aviso: 'Sin cruces', actividades: [{ id: '1' }] }) }
  }
  const resultado = await obtenerAgendaSugerida('token-estudiante')
  assert.equal(resultado.origen, 'ia')
  assert.equal(resultado.actividades.length, 1)
  assert.deepEqual(resultado.inscripciones, [])
})

test('informa sesión expirada y fallas de conexión', async (t) => {
  const original = globalThis.fetch
  t.after(() => { globalThis.fetch = original })
  globalThis.fetch = async () => ({ ok: false, status: 401 })
  await assert.rejects(obtenerAgendaSugerida('token'), /sesión expiró/)
  globalThis.fetch = async () => { throw new Error('ECONNREFUSED') }
  await assert.rejects(obtenerAgendaSugerida('token'), /conectar/)
})
