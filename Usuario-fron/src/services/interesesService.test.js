import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { ErrorIntereses, guardarMisIntereses, listarIntereses, obtenerMisIntereses } from './interesesService.js'

const original = globalThis.fetch
afterEach(() => {
  globalThis.fetch = original
})

/** fetch de mentira: guarda la última petición y responde lo que se le pida. */
function simular(estado, cuerpo) {
  const llamadas = []
  globalThis.fetch = async (url, opciones) => {
    llamadas.push({ url, opciones })
    return { ok: estado >= 200 && estado < 300, status: estado, json: async () => cuerpo }
  }
  return llamadas
}

test('SERVICIO: listarIntereses pide /api/intereses con el token', async () => {
  const llamadas = simular(200, { total: 2, intereses: [{ id: 'teatro', nombre: 'Teatro' }, { id: 'musica', nombre: 'Música' }] })
  const lista = await listarIntereses('TOKEN')
  assert.deepEqual(lista.map((i) => i.id), ['teatro', 'musica'])
  assert.ok(llamadas[0].url.endsWith('/api/intereses'))
  assert.equal(llamadas[0].opciones.headers.Authorization, 'Bearer TOKEN')
  assert.equal(llamadas[0].opciones.method, 'GET')
})

test('SERVICIO: obtenerMisIntereses devuelve la lista de ids', async () => {
  const llamadas = simular(200, { intereses: ['teatro'], actualizadoEn: null })
  assert.deepEqual(await obtenerMisIntereses('T'), ['teatro'])
  assert.ok(llamadas[0].url.endsWith('/api/intereses/mios'))
})

test('SERVICIO: sin intereses elegidos devuelve una lista vacía', async () => {
  simular(200, { intereses: [], actualizadoEn: null })
  assert.deepEqual(await obtenerMisIntereses('T'), [])
  simular(200, {})
  assert.deepEqual(await obtenerMisIntereses('T'), [])
})

test('SERVICIO: guardarMisIntereses hace PUT con { intereses } y devuelve lo guardado', async () => {
  const llamadas = simular(200, { intereses: ['teatro', 'musica'], actualizadoEn: '2026-10-15T10:00:00Z' })
  assert.deepEqual(await guardarMisIntereses('T', ['teatro', 'musica']), ['teatro', 'musica'])
  assert.equal(llamadas[0].opciones.method, 'PUT')
  assert.equal(llamadas[0].opciones.headers['Content-Type'], 'application/json')
  assert.deepEqual(JSON.parse(llamadas[0].opciones.body), { intereses: ['teatro', 'musica'] })
})

test('SERVICIO: un rechazo del servidor trae su mensaje y su estado', async () => {
  simular(400, { codigo: 'SELECCION_INVALIDA', mensaje: 'Elige al menos un interés.' })
  await assert.rejects(guardarMisIntereses('T', []), (e) => e instanceof ErrorIntereses && e.estado === 400 && e.message === 'Elige al menos un interés.')
})

test('SERVICIO: 401 avisa que la sesión expiró', async () => {
  simular(401, null)
  await assert.rejects(obtenerMisIntereses('T'), (e) => e.estado === 401 && /sesión expiró/.test(e.message))
})

test('SERVICIO: un error sin mensaje usa uno genérico', async () => {
  simular(500, null)
  await assert.rejects(listarIntereses('T'), (e) => e.estado === 500 && /No se pudo completar/.test(e.message))
})

test('SERVICIO: sin conexión avisa con estado 0', async () => {
  globalThis.fetch = async () => {
    throw new TypeError('fetch failed')
  }
  await assert.rejects(listarIntereses('T'), (e) => e instanceof ErrorIntereses && e.estado === 0 && /conectar/.test(e.message))
})
