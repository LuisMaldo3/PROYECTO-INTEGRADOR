import test from 'node:test'
import assert from 'node:assert/strict'
import {
  obtenerMisActividades, obtenerInscritos, guardarAsistencia, reportarProblema,
  actividadSeleccionada, rutaOrganizador,
} from './organizadorService.js'

// The service imports config.js via Vite import.meta.env: node:test requires a safe fallback.
test('rutas del organizador preservan el identificador', () => {
  assert.equal(actividadSeleccionada(rutaOrganizador('organizador-inscritos', 'id con /?')), 'id con /?')
  assert.equal(actividadSeleccionada('#/organizador-actividades'), '')
})

test('no realiza solicitudes sin sesión ni actividad', async () => {
  await assert.rejects(obtenerMisActividades(''), /Inicia sesión/)
  await assert.rejects(obtenerInscritos('token', ''), /Selecciona/)
  await assert.rejects(guardarAsistencia('token', 'a1', []), /No hay estudiantes/)
  await assert.rejects(reportarProblema('token', 'a1', 'otro', 'corto'), /15 y 2000/)
})

test('US-46 carga únicamente lo entregado por el backend con Bearer', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, options) => {
    assert.match(url, /\/api\/organizador\/actividades$/)
    assert.equal(options.headers.Authorization, 'Bearer token-de-prueba')
    return { ok: true, json: async () => ({ total: 1, actividades: [{ id: 'actividad-1', nombre: 'Actividad real' }] }) }
  }
  const actividades = await obtenerMisActividades('token-de-prueba')
  assert.deepEqual(actividades, [{ id: 'actividad-1', nombre: 'Actividad real' }])
})

test('US-51 interpreta inscritos reales y no crea datos simulados', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url) => {
    assert.match(url, /\/api\/organizador\/actividades\/a1\/inscritos$/)
    return { ok: true, json: async () => ({ actividad: { nombre: 'Debate' }, inscritos: [] }) }
  }
  const datos = await obtenerInscritos('token', 'a1')
  assert.deepEqual(datos, { actividad: { nombre: 'Debate' }, inscritos: [] })
})

test('US-55 envía exclusivamente marcas explícitas de asistencia', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, options) => {
    assert.match(url, /\/a1\/asistencia$/)
    assert.equal(options.method, 'POST')
    assert.deepEqual(JSON.parse(options.body), { asistencias: [{ estudianteId: 'e1', presente: false }] })
    return { ok: true, json: async () => ({ guardados: 1 }) }
  }
  assert.deepEqual(await guardarAsistencia('token', 'a1', [{ estudianteId: 'e1', presente: false }]), { guardados: 1 })
})

test('US-50 rechaza texto corto y envía un reporte de problema válido', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, options) => {
    assert.match(url, /\/a1\/reportes$/)
    assert.deepEqual(JSON.parse(options.body), { categoria: 'logistica', descripcion: 'Faltan micrófonos en el escenario.' })
    return { ok: true, json: async () => ({ id: 'reporte-1', estado: 'pendiente' }) }
  }
  assert.deepEqual(await reportarProblema('token', 'a1', 'logistica', '  Faltan micrófonos en el escenario.  '), { id: 'reporte-1', estado: 'pendiente' })
})

test('403, 404 y ausencia de red informan el error sin confundir con lista vacía', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async () => ({ ok: false, status: 403, json: async () => null })
  await assert.rejects(obtenerMisActividades('token'), /Solo una cuenta/)
  globalThis.fetch = async () => ({ ok: false, status: 404, json: async () => null })
  await assert.rejects(obtenerInscritos('token', 'ajena'), /no está asignada/)
  globalThis.fetch = async () => { throw new Error('offline') }
  await assert.rejects(obtenerMisActividades('token'), /Sin conexión/)
})
