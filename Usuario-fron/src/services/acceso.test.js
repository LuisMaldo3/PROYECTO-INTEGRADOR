import assert from 'node:assert/strict'
import { test } from 'node:test'
import { decidirAcceso, MENSAJES, tokenVigente } from './acceso.js'

const usuario = { id: '1', nombre: 'Ana Torres', correo: 'ana@tecsup.edu.pe', rol: 'usuario' }

test('PERMITE: el backend responde 200 con token y usuario', () => {
  const r = decidirAcceso({ estado: 200, datos: { token: 'jwt', usuario } })
  assert.equal(r.permitido, true)
  assert.deepEqual(r.sesion, { token: 'jwt', usuario })
})

test('BLOQUEA: correo que no es de Tecsup (403) y muestra el mensaje del backend', () => {
  const r = decidirAcceso({ estado: 403, datos: { codigo: 'DOMINIO_NO_PERMITIDO', mensaje: 'Usa tu correo @tecsup.edu.pe' } })
  assert.equal(r.permitido, false)
  assert.equal(r.codigo, 'DOMINIO_NO_PERMITIDO')
  assert.equal(r.mensaje, 'Usa tu correo @tecsup.edu.pe')
  assert.equal(r.sesion, undefined)
})

test('BLOQUEA: contraseña incorrecta (401)', () => {
  const r = decidirAcceso({ estado: 401, datos: { codigo: 'LOGIN_INCORRECTO', mensaje: 'Correo o contraseña incorrectos.' } })
  assert.equal(r.permitido, false)
  assert.equal(r.mensaje, 'Correo o contraseña incorrectos.')
})

test('BLOQUEA: rechazo sin cuerpo usa un mensaje por defecto', () => {
  const r = decidirAcceso({ estado: 401, datos: null })
  assert.equal(r.permitido, false)
  assert.equal(r.mensaje, MENSAJES.ERROR)
})

test('BLOQUEA: error del servidor (500)', () => {
  const r = decidirAcceso({ estado: 500, datos: null })
  assert.equal(r.permitido, false)
  assert.equal(r.codigo, 'ERROR_SERVIDOR')
  assert.equal(r.mensaje, MENSAJES.ERROR_SERVIDOR)
})

test('BLOQUEA: no hay conexión con el servidor', () => {
  const r = decidirAcceso({ errorDeRed: true })
  assert.equal(r.permitido, false)
  assert.equal(r.codigo, 'SIN_CONEXION')
})

test('BLOQUEA: 200 pero sin token o sin usuario (respuesta rara)', () => {
  assert.equal(decidirAcceso({ estado: 200, datos: { usuario } }).permitido, false)
  assert.equal(decidirAcceso({ estado: 200, datos: { token: 'jwt' } }).permitido, false)
  assert.equal(decidirAcceso({ estado: 200, datos: null }).permitido, false)
})

test('El rol no cambia la decisión: todas las cuentas entran como usuario', () => {
  const r = decidirAcceso({ estado: 200, datos: { token: 'jwt', usuario: { ...usuario, rol: 'usuario' } } })
  assert.equal(r.permitido, true)
  assert.equal(r.sesion.usuario.rol, 'usuario')
})

function jwtCon(payload) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url')
  return `${b64({ alg: 'HS256' })}.${b64(payload)}.firma`
}

test('tokenVigente: vence en el futuro -> sí; ya venció -> no; basura -> no', () => {
  const ahora = Date.parse('2026-10-12T12:00:00Z')
  assert.equal(tokenVigente(jwtCon({ exp: ahora / 1000 + 3600 }), ahora), true)
  assert.equal(tokenVigente(jwtCon({ exp: ahora / 1000 - 1 }), ahora), false)
  assert.equal(tokenVigente(jwtCon({ sub: '1' }), ahora), false) // sin exp
  assert.equal(tokenVigente('esto-no-es-un-jwt', ahora), false)
  assert.equal(tokenVigente(undefined, ahora), false)
})
