import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerAlternativasCruce } from './alternativasService.js'

test('alternativas: envía el JWT, codifica el cruce y recibe actividades reales', async (t) => {
  const original = globalThis.fetch
  t.after(() => { globalThis.fetch = original })
  globalThis.fetch = async (url, opts) => {
    assert.match(url, /\/api\/agenda\/cruces\/0-1\/alternativas$/)
    assert.equal(opts.headers.Authorization, 'Bearer token')
    return { ok: true, json: async () => ({ alternativas: [{ id: 'opt-1', fecha: '2026-12-01' }], total: 1, aviso: 'Sin cruces' }) }
  }
  const r = await obtenerAlternativasCruce('token', '0-1')
  assert.equal(r.alternativas[0].id, 'opt-1')
  assert.equal(r.aviso, 'Sin cruces')
})

test('alternativas: maneja lista vacía, expiración de sesión y errores', async (t) => {
  const original = globalThis.fetch
  t.after(() => { globalThis.fetch = original })
  globalThis.fetch = async () => ({ ok: true, json: async () => ({ alternativas: null }) })
  assert.deepEqual((await obtenerAlternativasCruce('token', '0-1')).alternativas, [])
  globalThis.fetch = async () => ({ ok: false, status: 401 })
  await assert.rejects(obtenerAlternativasCruce('token', '0-1'), /sesión expiró/)
  globalThis.fetch = async () => { throw Error('network') }
  await assert.rejects(obtenerAlternativasCruce('token', '0-1'), /conectar/)
  await assert.rejects(obtenerAlternativasCruce('', '0-1'), /Inicia sesión/)
})
