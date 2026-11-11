import test from 'node:test'
import assert from 'node:assert/strict'
import { actualizarDatosPermitidos, revisarResultadosArea } from './organizadorService.js'

test('US-48: solo permite editar descripción y reglamento; bloquea cupos y fechas', async () => {
  await assert.rejects(actualizarDatosPermitidos('token', 'act1', { maximoParticipantes: 40 }), /Solo puedes modificar/)
  await assert.rejects(actualizarDatosPermitidos('token', 'act1', { descripcion: 'corto' }), /15 y 2500/)
  await assert.rejects(actualizarDatosPermitidos('token', '', { descripcion: 'Una descripción de prueba válida' }), /Selecciona/)
})

test('US-48: PATCH restringido con token y confirmación del backend', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/api\/organizador\/actividades\/act1\/edicion$/)
    assert.equal(opciones.method, 'PATCH')
    assert.equal(opciones.headers.Authorization, 'Bearer token')
    assert.deepEqual(JSON.parse(opciones.body), { descripcion: 'Descripción oficial autorizada' })
    return { ok: true, json: async () => ({ id: 'act1', descripcion: 'Descripción oficial autorizada' }) }
  }
  const recibido = await actualizarDatosPermitidos('token', 'act1', { descripcion: '  Descripción oficial autorizada  ' })
  assert.equal(recibido.id, 'act1')
})

test('US-48: revisión de coordinadores de área no envía autorización de publicación', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/resultados\/revision-area$/)
    assert.equal(opciones.method, 'POST')
    assert.deepEqual(JSON.parse(opciones.body), { estado: 'observado', observacion: 'El puntaje no coincide con el acta.' })
    return { ok: true, json: async () => ({ estado: 'pendiente' }) }
  }
  assert.deepEqual(await revisarResultadosArea('token', 'act1', 'observado', 'El puntaje no coincide con el acta.'), { estado: 'pendiente' })
  await assert.rejects(revisarResultadosArea('token', 'act1', 'publicado', 'texto de prueba'), /Conforme u Observado/)
  await assert.rejects(revisarResultadosArea('token', 'act1', 'observado', 'breve'), /10 a 800/)
})
