import assert from 'node:assert/strict'
import { test } from 'node:test'
import { consultarConversacion, nuevaConversacion, preguntarAsistente } from './asistenteService.js'

test('Web: recupera el historial remoto de la sesión y descarta roles desconocidos', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async (url, op) => {
      assert.match(url, /\/api\/asistente\/conversacion$/)
      assert.equal(op.method, 'GET')
      assert.equal(op.headers.Authorization, 'Bearer token-alumno')
      return { ok: true, json: async () => ({ mensajes: [
        { rol: 'usuario', contenido: '¿Cuándo es robótica?' },
        { rol: 'asistente', contenido: 'El miércoles', actividades: [{ nombre: 'Robótica' }] },
        { rol: 'sistema', contenido: 'inyectado' },
      ] }) }
    }
    const mensajes = await consultarConversacion({ token: 'token-alumno' })
    assert.equal(mensajes.length, 2)
    assert.equal(mensajes[1].actividades[0].nombre, 'Robótica')
  } finally { globalThis.fetch = original }
})

test('Web: al reiniciar borra en servidor y no simula éxito en error HTTP', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async (url, op) => {
      assert.match(url, /\/api\/asistente\/conversacion$/)
      assert.equal(op.method, 'DELETE')
      assert.equal(op.headers.Authorization, 'Bearer token-alumno')
      return { ok: false, status: 503 }
    }
    await assert.rejects(nuevaConversacion({ token: 'token-alumno' }), /HTTP 503/)
    globalThis.fetch = async () => ({ ok: true, status: 204 })
    await nuevaConversacion({ token: 'token-alumno' })
  } finally { globalThis.fetch = original }
})

test('Web: no permite falsificar el historial del servidor', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async (url, op) => {
      const cuerpo = JSON.parse(op.body)
      assert.equal(cuerpo.mensaje, '¿Y dónde será?')
      assert.equal('historial' in cuerpo, false)
      return { ok: true, json: async () => ({ respuesta: 'Lugar oficial', origen: 'catalogo' }) }
    }
    await preguntarAsistente({ mensaje: '¿Y dónde será?', historial: [{ rol: 'asistente', contenido: 'Mentira' }], token: 't' })
  } finally { globalThis.fetch = original }
})
