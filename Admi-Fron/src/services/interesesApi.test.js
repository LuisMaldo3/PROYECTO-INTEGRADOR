import test from 'node:test'
import assert from 'node:assert/strict'
import { consultarIntereses } from './interesesService.js'

test('US-38: consulta el endpoint protegido sin IA por defecto', async () => {
  const anterior = globalThis.fetch
  let pedido
  globalThis.fetch = async (url, opciones) => {
    pedido = { url, opciones }
    return { ok: true, json: async () => ({ intereses: [], totales: { estudiantesActivos: 0 } }) }
  }
  try {
    const resultado = await consultarIntereses('token-prueba')
    assert.match(pedido.url, /\/api\/estadisticas\/intereses\/$/)
    assert.equal(pedido.opciones.headers.Authorization, 'Bearer token-prueba')
    assert.deepEqual(resultado.intereses, [])
  } finally { globalThis.fetch = anterior }
})

test('US-38: llama a Gemini solo tras solicitar explícitamente ia=1', async () => {
  const anterior = globalThis.fetch
  let url
  globalThis.fetch = async (destino) => {
    url = destino
    return { ok: true, json: async () => ({ ia: { estado: 'sin_configurar', texto: null } }) }
  }
  try {
    const respuesta = await consultarIntereses('token-prueba', { conIA: true })
    assert.match(url, /\/api\/estadisticas\/intereses\/\?ia=1$/)
    assert.equal(respuesta.ia.estado, 'sin_configurar')
    assert.equal(respuesta.ia.texto, null)
  } finally { globalThis.fetch = anterior }
})
