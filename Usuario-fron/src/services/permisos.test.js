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
const TODAS = ['inicio', 'catalogo', 'agenda', 'asistente', 'perfil']

// Lista de prueba con una pantalla solo del organizador (las reales se agregarán más adelante)
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

test('MENÚ: el usuario ve las cinco pantallas, en orden', () => {
  assert.deepEqual(rutas('usuario'), TODAS)
})

test('MENÚ: por ahora todos los roles ven las pantallas del usuario', () => {
  assert.deepEqual(rutas('organizador'), TODAS)
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
  assert.deepEqual(rutas('organizador', CON_ORGANIZADOR), [...TODAS, 'mis-actividades'])
  assert.deepEqual(rutas('usuario', CON_ORGANIZADOR), TODAS)
})

test('BLOQUEO: si el rol cambia, la misma dirección pasa de permitida a bloqueada', () => {
  assert.equal(resolverRuta('#/mis-actividades', 'organizador', CON_ORGANIZADOR).estado, 'permitida')
  assert.equal(resolverRuta('#/mis-actividades', 'usuario', CON_ORGANIZADOR).estado, 'bloqueada')
})
