import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerMiComentario, guardarMiComentario, obtenerEstadisticasComentarios, generarAnalisisComentarios } from './comentariosService.js'

test('US-62: un participante consulta su comentario con sesión', async t => {
  const anterior = globalThis.fetch; t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opts) => {
    assert.match(url, /\/api\/actividades\/a1\/comentarios\/mio$/)
    assert.equal(opts.headers.Authorization, 'Bearer JWT')
    assert.equal(opts.method, 'GET')
    return { ok: true, json: async () => ({ puedeComentar: true, comentario: 'Me gustó el taller' }) }
  }
  assert.equal((await obtenerMiComentario('JWT', 'a1')).puedeComentar, true)
})

test('US-62: guardar comentario solo con texto válido y sin enviar claves de IA', async t => {
  const anterior = globalThis.fetch; t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opts) => {
    assert.match(url, /\/api\/actividades\/a1\/comentarios$/)
    assert.equal(opts.headers.Authorization, 'Bearer JWT')
    assert.equal(opts.headers['x-goog-api-key'], undefined)
    assert.deepEqual(JSON.parse(opts.body), { texto: 'Muy bien organizado' })
    return { ok: true, json: async () => ({ guardado: true }) }
  }
  assert.equal((await guardarMiComentario('JWT', 'a1', ' Muy bien organizado ')).guardado, true)
  await assert.rejects(guardarMiComentario('JWT', 'a1', 'corto'), /10 y 1000/)
})

test('US-62/63: organizador obtiene contador y análisis mediante POST', async t => {
  const anterior = globalThis.fetch; t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opts) => {
    assert.equal(opts.headers.Authorization, 'Bearer JWT')
    if (url.endsWith('/estadisticas')) {
      assert.equal(opts.method, 'GET')
      return { ok: true, json: async () => ({ totalComentarios: 3 }) }
    }
    assert.match(url, /\/api\/organizador\/actividades\/a1\/comentarios\/analisis-ia$/)
    assert.equal(opts.method, 'POST')
    return { ok: true, json: async () => ({ resumen: 'Muy buena organización', temas: [{ tema: 'Horarios', cantidad: 2 }], origen: 'gemini' }) }
  }
  assert.equal((await obtenerEstadisticasComentarios('JWT', 'a1')).totalComentarios, 3)
  const res = await generarAnalisisComentarios('JWT', 'a1')
  assert.equal(res.temas[0].cantidad, 2)
})

test('US-63: rechaza actividades ajenas y nunca simula resultado IA en error', async t => {
  const anterior = globalThis.fetch; t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async () => ({ ok: false, status: 403, json: async () => ({}) })
  await assert.rejects(generarAnalisisComentarios('JWT', 'ajena'), /No tienes permiso/)
  globalThis.fetch = async () => { throw new Error('offline') }
  await assert.rejects(obtenerEstadisticasComentarios('JWT', 'a1'), /No se pudo conectar/)
})
