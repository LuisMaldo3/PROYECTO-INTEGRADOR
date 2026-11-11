import test from 'node:test'
import assert from 'node:assert/strict'
import { obtenerOcupacionActividad, verificarEquiposActividad } from './organizadorService.js'
import { resolverRuta } from './permisos.js'

test('US-58 consulta ocupación real con sesión y sin modificar cupos', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/api\/organizador\/actividades\/a1\/ocupacion$/)
    assert.equal(opciones.headers.Authorization, 'Bearer token')
    assert.equal(opciones.method, 'GET')
    assert.equal(opciones.body, undefined)
    return { ok: true, json: async () => ({ cuposOcupados: 2, cuposDisponibles: 3, unidadCupo: 'equipos', estudiantesInscritos: 8 }) }
  }
  const datos = await obtenerOcupacionActividad('token', 'a1')
  assert.equal(datos.cuposDisponibles, 3)
  assert.equal(datos.estudiantesInscritos, 8)
})

test('US-59 verifica cada equipo por API autorizada y conserva detalle individual', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async (url, opciones) => {
    assert.match(url, /\/api\/organizador\/actividades\/torneo\/equipos\/verificacion$/)
    assert.equal(opciones.headers.Authorization, 'Bearer token')
    return { ok: true, json: async () => ({ totalEquipos: 1, equiposCompletos: 0, equiposPendientes: 1,
      equipos: [{ equipoId: 'e1', nombre: 'Digital', integrantes: [{ nombre: 'Ana', inscritoIndividualmente: false }] }] }) }
  }
  const datos = await verificarEquiposActividad('token', 'torneo')
  assert.equal(datos.equiposPendientes, 1)
  assert.equal(datos.equipos[0].integrantes[0].inscritoIndividualmente, false)
})

test('US-58 y US-59 no permiten consultas sin actividad y protegen la ruta por rol', async () => {
  await assert.rejects(obtenerOcupacionActividad('token', ''), /Selecciona/)
  await assert.rejects(verificarEquiposActividad('token', ''), /Selecciona/)
  assert.equal(resolverRuta('#/organizador-cupos?actividad=a1', 'usuario').estado, 'bloqueada')
  assert.equal(resolverRuta('#/organizador-cupos?actividad=a1', 'organizador').estado, 'permitida')
})

test('errores de autorización y conexión no se convierten en estadísticas vacías', async t => {
  const anterior = globalThis.fetch
  t.after(() => { globalThis.fetch = anterior })
  globalThis.fetch = async () => ({ ok: false, status: 403, json: async () => ({}) })
  await assert.rejects(obtenerOcupacionActividad('token', 'ajena'), /Solo una cuenta/)
  globalThis.fetch = async () => { throw new Error('offline') }
  await assert.rejects(verificarEquiposActividad('token', 'a1'), /Sin conexión/)
})
