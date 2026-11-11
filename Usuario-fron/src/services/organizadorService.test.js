import test from 'node:test'
import assert from 'node:assert/strict'
import {
  obtenerMisActividades, obtenerInscritos, reemplazarInscrito, reportarProblema,
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
  await assert.rejects(reemplazarInscrito('token', 'a1', '', 'nuevo@tecsup.edu.pe', 'Sustitución'), /Selecciona/)
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
  assert.deepEqual(datos, { actividad: { nombre: 'Debate' }, inscritos: [], equiposRegistrados: 0, equiposConfirmados: 0 })
})

test('US-51 reemplaza un inscrito sin solicitar ni modificar el cupo', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, options) => {
    assert.match(url, /\/a1\/inscritos\/reemplazar$/)
    assert.equal(options.method, 'POST')
    assert.deepEqual(JSON.parse(options.body), {
      inscripcionId: 'i1', correoNuevo: 'nuevo@tecsup.edu.pe', motivo: 'Corrección de equipo',
    })
    return { ok: true, json: async () => ({ reemplazado: true, cuposModificados: false }) }
  }
  assert.deepEqual(await reemplazarInscrito('token', 'a1', 'i1', 'NUEVO@tecsup.edu.pe', 'Corrección de equipo'),
    { reemplazado: true, cuposModificados: false })
})

test('US-51 exige correo de Tecsup y motivo para reemplazar', async () => {
  await assert.rejects(reemplazarInscrito('token', 'a1', 'i1', 'otro@gmail.com', 'Cambio válido'), /institucional/)
  await assert.rejects(reemplazarInscrito('token', 'a1', 'i1', 'nuevo@tecsup.edu.pe', 'abc'), /motivo/)
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

test('RESULTADOS: el organizador envía posiciones pendientes con JWT, sin publicarlas', async t => {
  const { obtenerResultadosOrganizador, enviarResultadosOrganizador } = await import('./organizadorService.js')
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  const llamadas = []
  globalThis.fetch = async (url, opciones) => {
    llamadas.push({ url, opciones })
    return { ok: true, json: async () => ({ estado: 'pendiente', posiciones: [{ inscripcionId: 'i1', puesto: 1 }] }) }
  }
  const propuesta = [{ inscripcionId: 'i1', puesto: 1 }]
  const respuesta = await enviarResultadosOrganizador('token-organizador', 'actividad-123', propuesta)
  assert.equal(respuesta.estado, 'pendiente')
  assert.match(llamadas[0].url, /\/api\/organizador\/actividades\/actividad-123\/resultados$/)
  assert.equal(llamadas[0].opciones.headers.Authorization, 'Bearer token-organizador')
  assert.equal(llamadas[0].opciones.method, 'PUT')
  assert.deepEqual(JSON.parse(llamadas[0].opciones.body), { posiciones: propuesta })
  await obtenerResultadosOrganizador('token-organizador', 'actividad-123')
  assert.equal(llamadas[1].opciones.method, 'GET')
})

test('RESULTADOS: un organizador sin actividad no puede registrar puestos', async () => {
  const { enviarResultadosOrganizador } = await import('./organizadorService.js')
  await assert.rejects(enviarResultadosOrganizador('token', '', [{ inscripcionId: 'i', puesto: 1 }]), /Selecciona/)
})


test('SPRINT 1-2: lista de equipos confirmados conserva métricas reales del backend', async t => {
  const previo = globalThis.fetch
  t.after(() => { globalThis.fetch = previo })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /buscar=Miner%C3%ADa/)
    assert.equal(opciones.headers.Authorization, 'Bearer token')
    return { ok: true, json: async () => ({ actividad: { unidadCupo: 'equipos' },
      totalInscritos: 5, equiposRegistrados: 3, equiposConfirmados: 2,
      inscritos: [{ nombre: 'Ana', areaTecsup: 'Minería y Procesos Químicos', equipoHabilitado: true }] }) }
  }
  const respuesta = await obtenerInscritos('token', 'a1', undefined, 'Minería')
  assert.equal(respuesta.equiposConfirmados, 2)
  assert.equal(respuesta.equiposRegistrados, 3)
  assert.equal(respuesta.totalInscritos, 5)
  assert.equal(respuesta.inscritos[0].equipoHabilitado, true)
})

test('US-57 consulta el historial de la actividad con JWT, sin añadir filtros de la lista vigente', async t => {
  const { obtenerHistorialReemplazos } = await import('./organizadorService.js')
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  const cambios = [{ inscripcionId: 'i1', estudianteRetirado: 'Elena', estudianteNuevo: 'Ana', motivo: 'Cambio autorizado', fecha: '2026-11-11T16:00:00Z' }]
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/api\/organizador\/actividades\/actividad-1\/inscritos\/historial$/)
    assert.equal(opciones.headers.Authorization, 'Bearer token-real')
    assert.equal(opciones.method, 'GET')
    return { ok: true, json: async () => ({ total: 1, historial: cambios }) }
  }
  assert.deepEqual(await obtenerHistorialReemplazos('token-real', 'actividad-1'), { historial: cambios, total: 1 })
})

test('US-57 sin actividad o sin permiso no devuelve un historial inventado', async t => {
  const { obtenerHistorialReemplazos } = await import('./organizadorService.js')
  await assert.rejects(obtenerHistorialReemplazos('token', ''), /Selecciona/)
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async () => ({ ok: false, status: 404, json: async () => ({}) })
  await assert.rejects(obtenerHistorialReemplazos('token', 'ajena'), /no está asignada/)
})

test('US-57 historial vacío se devuelve como lista vacía, no como sustituciones supuestas', async t => {
  const { obtenerHistorialReemplazos } = await import('./organizadorService.js')
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async () => ({ ok: true, json: async () => ({ total: 0, historial: [] }) })
  assert.deepEqual(await obtenerHistorialReemplazos('token', 'a1'), { total: 0, historial: [] })
})
