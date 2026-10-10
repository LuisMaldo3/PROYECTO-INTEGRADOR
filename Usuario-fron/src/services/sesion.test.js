import assert from 'node:assert/strict'
import { test } from 'node:test'
import { esAdministrador, leerClaims, normalizarRol, tokenVigente } from './sesion.js'

const aBase64Url = (obj) =>
  Buffer.from(JSON.stringify(obj)).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
const jwt = (claims) => `${aBase64Url({ alg: 'HS256' })}.${aBase64Url(claims)}.firma`

test('TOKEN: lee los datos del JWT', () => {
  const claims = leerClaims(jwt({ sub: '1', rol: 'administrador', exp: 100 }))
  assert.equal(claims.sub, '1')
  assert.equal(claims.rol, 'administrador')
})

test('TOKEN: lo que no es un JWT devuelve null', () => {
  assert.equal(leerClaims('basura'), null)
  assert.equal(leerClaims(undefined), null)
})

test('TOKEN: vigente solo si no venció', () => {
  assert.equal(tokenVigente(jwt({ exp: 2000 }), 1000), true)
  assert.equal(tokenVigente(jwt({ exp: 900 }), 1000), false)
  assert.equal(tokenVigente(jwt({}), 1000), false)
  assert.equal(tokenVigente('basura', 1000), false)
})

test('ROL: solo "administrador" (sin importar mayúsculas) es admin', () => {
  assert.equal(normalizarRol('  ADMINISTRADOR '), 'administrador')
  assert.equal(esAdministrador({ rol: 'Administrador' }), true)
  assert.equal(esAdministrador({ rol: 'organizador' }), false)
  assert.equal(esAdministrador({ rol: 'usuario' }), false)
  assert.equal(esAdministrador(null), false)
})
