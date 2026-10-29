import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerDetalleActividad } from './organizadorService.js'

test('el detalle utiliza JWT, ID codificado y respeta la respuesta real', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/api\/organizador\/actividades\/actividad%20uno$/)
    assert.equal(opciones.headers.Authorization, 'Bearer token')
    return { ok: true, json: async () => ({ id: 'actividad uno', nombre: 'Debate', inscritosRegistrados: 3 }) }
  }
  assert.deepEqual(await obtenerDetalleActividad('token', 'actividad uno'), { id: 'actividad uno', nombre: 'Debate', inscritosRegistrados: 3 })
})

test('sin actividad no consulta detalles', async () => {
  await assert.rejects(obtenerDetalleActividad('token', ''), /Selecciona/)
})
