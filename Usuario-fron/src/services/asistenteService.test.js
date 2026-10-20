import assert from 'node:assert/strict'
import { test } from 'node:test'
import { normalizarUbicaciones, normalizarActividadesMomento, preguntarAsistente } from './asistenteService.js'

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

test('Asistente: presenta la ubicación publicada y reconoce ubicación por confirmar', () => {
  const lugares = normalizarUbicaciones([
    { actividadId: 'a1', nombre: 'Robótica', lugar: 'Auditorio Tecsup', aula: 'A-201', confirmada: true },
    { actividadId: 'a2', nombre: 'Taller', lugar: 'Ubicación por confirmar', confirmada: false },
  ])
  assert.equal(lugares[0].aula, 'A-201')
  assert.equal(lugares[0].confirmada, true)
  assert.equal(lugares[1].confirmada, false)
})

test('Asistente: descarta datos de ubicación malformados y acota el tamaño', () => {
  assert.deepEqual(normalizarUbicaciones(null), [])
  assert.equal(normalizarUbicaciones([{ nombre: 'X'.repeat(500) }, null]).length, 1)
  assert.equal(normalizarUbicaciones([{ nombre: 'X'.repeat(500) }])[0].nombre.length, 300)
})


test('Asistente: muestra solo actividades verificables de una consulta temporal', () => {
  const lista = normalizarActividadesMomento([
    { id: 'a1', nombre: 'Robótica', fecha: '2026-10-26', horaInicio: '14:00', horaFin: '16:00', cuposDisponibles: 5 },
    { id: 'a2', nombre: 'Sin fecha' }, null,
  ])
  assert.equal(lista.length, 1)
  assert.equal(lista[0].cuposDisponibles, 5)
  assert.equal(lista[0].nombre, 'Robótica')
})

test('Asistente: limita los resultados y no inventa cupos desconocidos', () => {
  const lista = normalizarActividadesMomento(Array.from({ length: 12 }, (_, n) => ({
    id: `${n}`, nombre: `Taller ${n}`, fecha: '2026-10-26', horaInicio: '09:00', horaFin: '11:00', cuposDisponibles: 'sin dato',
  })))
  assert.equal(lista.length, 7)
  assert.equal(lista[0].cuposDisponibles, null)
})
