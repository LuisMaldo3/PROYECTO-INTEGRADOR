import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerParticipacionOrganizador, generarResumenOrganizadorIA } from './organizadorService.js'

test('US-61: obtiene solo los datos de su actividad usando JWT', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opts) => {
    assert.match(url, /\/api\/organizador\/actividades\/a1\/participacion$/)
    assert.equal(opts.headers.Authorization, 'Bearer JWT')
    assert.equal(opts.method, 'GET')
    assert.equal(opts.body, undefined)
    return { ok: true, json: async () => ({ porcentajeParticipacion: 75, cuposOcupados: 3,
      maximoParticipantes: 4, unidadCupo: 'equipos', estudiantesInscritos: 10 }) }
  }
  const datos = await obtenerParticipacionOrganizador('JWT', 'a1')
  assert.equal(datos.porcentajeParticipacion, 75)
  assert.equal(datos.estudiantesInscritos, 10)
})

test('US-60: genera IA solamente al solicitarlo, sin enviar clave ni nombres', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opts) => {
    assert.match(url, /\/api\/organizador\/actividades\/competencia\/resumen-inscripciones-ia$/)
    assert.equal(opts.headers.Authorization, 'Bearer JWT')
    assert.equal(opts.method, 'POST')
    assert.equal(opts.body, undefined)
    assert.equal(opts.headers['x-goog-api-key'], undefined)
    return { ok: true, json: async () => ({ resumen: 'Datos de prueba', origen: 'gemini', datos: {} }) }
  }
  const datos = await generarResumenOrganizadorIA('JWT', 'competencia')
  assert.equal(datos.origen, 'gemini')
})

test('ambas operaciones requieren una actividad seleccionada', async () => {
  await assert.rejects(obtenerParticipacionOrganizador('JWT', ''), /Selecciona/)
  await assert.rejects(generarResumenOrganizadorIA('JWT', ''), /Selecciona/)
})

test('errores 401/403 y caída de backend nunca simulan datos o un resumen IA', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async () => ({ ok: false, status: 403, json: async () => ({}) })
  await assert.rejects(generarResumenOrganizadorIA('JWT', 'ajena'), /Solo una cuenta/)
  globalThis.fetch = async () => { throw new Error('offline') }
  await assert.rejects(obtenerParticipacionOrganizador('JWT', 'a1'), /Sin conexión/)
})
