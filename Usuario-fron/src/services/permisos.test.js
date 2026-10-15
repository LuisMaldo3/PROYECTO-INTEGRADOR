import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  normalizarRol,
  normalizarUsuario,
  pantallasDelMenu,
  PANTALLAS,
  puedeVer,
  resolverRuta,
  rutaDeHash,
} from './permisos.js'

const rutas = (rol, lista) => pantallasDelMenu(rol, lista).map((p) => p.ruta)
const TODAS = ['inicio', 'catalogo', 'recomendaciones', 'agenda', 'asistente', 'perfil']

// Lista de prueba con una segunda pantalla hipotética del organizador
const CON_ORGANIZADOR = [
  ...PANTALLAS,
  { ruta: 'mis-actividades', titulo: 'Mis actividades', tituloMenu: 'Mis actividades', roles: ['organizador'] },
]

test('ROL: se guarda en minúsculas y lo desconocido cuenta como "usuario"', () => {
  assert.equal(normalizarRol('Organizador'), 'organizador')
  assert.equal(normalizarRol('  ADMINISTRADOR '), 'administrador')
  assert.equal(normalizarRol('superadmin'), 'usuario')
  assert.equal(normalizarRol(''), 'usuario')
  assert.equal(normalizarRol(undefined), 'usuario')
  assert.equal(normalizarRol(null), 'usuario')
  assert.equal(normalizarRol(42), 'usuario')
})

test('ROL: normalizarUsuario no pierde los demás datos ni modifica el original', () => {
  const original = { id: '1', nombre: 'Ana', correo: 'ana@tecsup.edu.pe', rol: 'ORGANIZADOR' }
  const r = normalizarUsuario(original)
  assert.deepEqual(r, { ...original, rol: 'organizador' })
  assert.equal(original.rol, 'ORGANIZADOR')
  assert.equal(normalizarUsuario(null), null)
})

test('MENÚ: el usuario ve las seis pantallas, en orden', () => {
  assert.deepEqual(rutas('usuario'), TODAS)
})

test('MENÚ: el organizador ve sus actividades y los demás roles no', () => {
  assert.deepEqual(rutas('organizador'), [...TODAS, 'organizador-actividades'])
  assert.deepEqual(rutas('administrador'), TODAS)
  assert.deepEqual(rutas('hacker'), TODAS)
  assert.deepEqual(rutas(undefined), TODAS)
})

test('MENÚ: Perfil está siempre, porque ahí se cierra la sesión', () => {
  assert.ok(rutas('usuario').includes('perfil'))
})

test('RUTA: se lee de la dirección (#/...) y sin nada es inicio', () => {
  assert.equal(rutaDeHash(''), 'inicio')
  assert.equal(rutaDeHash('#'), 'inicio')
  assert.equal(rutaDeHash('#/'), 'inicio')
  assert.equal(rutaDeHash('#/Agenda'), 'agenda')
  assert.equal(rutaDeHash('#/agenda/5?x=1'), 'agenda')
  assert.equal(rutaDeHash(undefined), 'inicio')
})

test('RUTA: las pantallas del usuario están permitidas', () => {
  for (const hash of ['', '#/inicio', '#/catalogo', '#/agenda', '#/asistente', '#/perfil']) {
    assert.equal(resolverRuta(hash, 'usuario').estado, 'permitida', hash)
  }
})

test('RUTA: una pantalla que no existe (como #/usuarios o #/organizador) es inexistente', () => {
  for (const hash of ['#/usuarios', '#/organizador', '#/nada']) {
    const r = resolverRuta(hash, 'usuario')
    assert.equal(r.estado, 'inexistente', hash)
    assert.equal(r.pantalla, null)
  }
  assert.equal(puedeVer('administrador', 'constructor'), false)
})

test('BLOQUEO: una pantalla de organizador queda bloqueada para el usuario', () => {
  const r = resolverRuta('#/mis-actividades', 'usuario', CON_ORGANIZADOR)
  assert.equal(r.estado, 'bloqueada')
  assert.equal(r.pantalla.titulo, 'Mis actividades')
})

test('BLOQUEO: el organizador sí entra a su pantalla y ve su menú', () => {
  assert.equal(resolverRuta('#/mis-actividades', 'organizador', CON_ORGANIZADOR).estado, 'permitida')
  assert.deepEqual(rutas('organizador', CON_ORGANIZADOR), [...TODAS, 'organizador-actividades', 'mis-actividades'])
  assert.deepEqual(rutas('usuario', CON_ORGANIZADOR), TODAS)
})

test('BLOQUEO: si el rol cambia, la misma dirección pasa de permitida a bloqueada', () => {
  assert.equal(resolverRuta('#/mis-actividades', 'organizador', CON_ORGANIZADOR).estado, 'permitida')
  assert.equal(resolverRuta('#/mis-actividades', 'usuario', CON_ORGANIZADOR).estado, 'bloqueada')
})

test('INTERESES: es una pantalla permitida, pero no tiene botón en el menú', () => {
  assert.equal(resolverRuta('#/intereses', 'usuario').estado, 'permitida')
  assert.equal(rutas('usuario').includes('intereses'), false)
  assert.deepEqual(rutas('usuario'), TODAS) // el menú sigue con las mismas seis
})

test('INTERESES: mientras se está en ella, el menú marca Perfil', () => {
  const r = resolverRuta('#/intereses', 'usuario')
  assert.equal(r.pantalla.menuActivo, 'perfil')
  assert.equal(r.pantalla.titulo, 'Mis intereses')
})

test('MENÚ: Para ti aparece una sola vez', () => {
  assert.equal(rutas('usuario').filter(r => r === 'recomendaciones').length, 1)
})


test('US-46/51/55/50: organizador accede a todas sus pantallas y usuario queda bloqueado', () => {
  for (const ruta of ['organizador-actividades', 'organizador-inscritos', 'organizador-asistencia', 'organizador-reporte']) {
    assert.equal(resolverRuta(`#/${ruta}?actividad=abc`, 'organizador').estado, 'permitida')
    assert.equal(resolverRuta(`#/${ruta}?actividad=abc`, 'usuario').estado, 'bloqueada')
    assert.equal(resolverRuta(`#/${ruta}?actividad=abc`, 'administrador').estado, 'bloqueada')
  }
  assert.equal(rutas('organizador').filter(r => r.startsWith('organizador')).length, 1)
  assert.equal(resolverRuta('#/organizador-inscritos', 'organizador').pantalla.menuActivo, 'organizador-actividades')
})
