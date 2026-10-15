import assert from 'node:assert/strict'
import { test } from 'node:test'
import { preguntarAsistente } from './asistenteService.js'

const anterior = globalThis.fetch

test('Asistente: usa el endpoint real con JWT y contexto limitado', async () => {
  globalThis.fetch = async (url, options) => {
    assert.match(url, /\/api\/asistente\/preguntar$/)
    assert.equal(options.headers.Authorization, 'Bearer token-prueba')
    assert.equal(JSON.parse(options.body).mensaje, '¿Qué actividades hay hoy?')
    return { ok: true, json: async () => ({ respuesta: 'Horarios reales', origen: 'catalogo' }) }
  }
  try {
    const r = await preguntarAsistente({ token: 'token-prueba', mensaje: '¿Qué actividades hay hoy?' })
    assert.equal(r.respuesta, 'Horarios reales')
  } finally { globalThis.fetch = anterior }
})

test('Asistente: 404 informa que falta instalar el backend', async () => {
  globalThis.fetch = async () => ({ ok: false, status: 404 })
  try { await assert.rejects(preguntarAsistente({ token: 't', mensaje: '¿Qué hay?' }), /Actualiza el backend/) }
  finally { globalThis.fetch = anterior }
})

test('Asistente: no envía una pregunta vacía', async () => {
  await assert.rejects(preguntarAsistente({ token: 't', mensaje: '   ' }), /hasta 600 caracteres/)
})
