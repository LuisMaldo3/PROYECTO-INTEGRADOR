import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  normalizarRol,
  normalizarUsuario,
  pantallasDelMenu,
  puedeVer,
  resolverRuta,
  rutaDeHash,
} from './permisos.js'

const rutas = (rol) => pantallasDelMenu(rol).map((p) => p.ruta)

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

test('MENÚ: cada rol ve solo sus pantallas', () => {
  assert.deepEqual(rutas('usuario'), ['inicio'])
  assert.deepEqual(rutas('organizador'), ['inicio', 'organizador'])
  assert.deepEqual(rutas('administrador'), ['inicio', 'organizador', 'usuarios'])
})

test('MENÚ: un rol raro se trata como usuario', () => {
  assert.deepEqual(rutas('hacker'), ['inicio'])
  assert.deepEqual(rutas(undefined), ['inicio'])
})

test('PERMISOS: quién puede ver qué', () => {
  assert.equal(puedeVer('usuario', 'organizador'), false)
  assert.equal(puedeVer('usuario', 'usuarios'), false)
  assert.equal(puedeVer('organizador', 'organizador'), true)
  assert.equal(puedeVer('organizador', 'usuarios'), false)
  assert.equal(puedeVer('administrador', 'usuarios'), true)
  assert.equal(puedeVer('administrador', 'organizador'), true)
})

test('PERMISOS: una pantalla que no existe no la ve nadie', () => {
  assert.equal(puedeVer('administrador', 'secreta'), false)
  assert.equal(puedeVer('administrador', 'constructor'), false)
})

test('RUTA: se lee de la dirección (#/...) y sin nada es inicio', () => {
  assert.equal(rutaDeHash(''), 'inicio')
  assert.equal(rutaDeHash('#'), 'inicio')
  assert.equal(rutaDeHash('#/'), 'inicio')
  assert.equal(rutaDeHash('#/Usuarios'), 'usuarios')
  assert.equal(rutaDeHash('#/usuarios/5?x=1'), 'usuarios')
  assert.equal(rutaDeHash(undefined), 'inicio')
})

test('BLOQUEO: entrar por la dirección a una pantalla de otro rol queda bloqueado', () => {
  const r = resolverRuta('#/usuarios', 'usuario')
  assert.equal(r.estado, 'bloqueada')
  assert.equal(r.ruta, 'usuarios')
  assert.equal(r.pantalla.titulo, 'Gestión de usuarios')
})

test('BLOQUEO: el organizador no entra a la gestión de usuarios', () => {
  assert.equal(resolverRuta('#/usuarios', 'organizador').estado, 'bloqueada')
  assert.equal(resolverRuta('#/organizador', 'organizador').estado, 'permitida')
})

test('BLOQUEO: el administrador entra a todo', () => {
  for (const hash of ['', '#/inicio', '#/organizador', '#/usuarios']) {
    assert.equal(resolverRuta(hash, 'administrador').estado, 'permitida', hash)
  }
})

test('BLOQUEO: pantalla que no existe', () => {
  const r = resolverRuta('#/nada', 'administrador')
  assert.equal(r.estado, 'inexistente')
  assert.equal(r.pantalla, null)
})

test('BLOQUEO: si el rol cambia, la misma dirección pasa de permitida a bloqueada', () => {
  assert.equal(resolverRuta('#/organizador', 'organizador').estado, 'permitida')
  assert.equal(resolverRuta('#/organizador', 'usuario').estado, 'bloqueada')
})
