import assert from 'node:assert/strict'
import { test } from 'node:test'
import { borrarDatosDeSesion, sinTokenGuardado } from './sesionLocal.js'

/** Almacén de mentira con la misma interfaz que localStorage. */
function almacen(datos) {
  const mapa = new Map(Object.entries(datos))
  return {
    get length() { return mapa.size },
    key: (i) => [...mapa.keys()][i] ?? null,
    getItem: (k) => mapa.get(k) ?? null,
    removeItem: (k) => mapa.delete(k),
    claves: () => [...mapa.keys()],
  }
}

test('BORRAR SESIÓN: quita el token y el usuario', () => {
  const local = almacen({ 'vynk.token': 't', 'vynk.usuario': '{}' })
  assert.equal(borrarDatosDeSesion([local]), 2)
  assert.deepEqual(local.claves(), [])
})

test('BORRAR SESIÓN: también quita lo que se agregue después con el prefijo vynk. (p. ej. el chat)', () => {
  const local = almacen({ 'vynk.token': 't', 'vynk.chat.conversacion': '[]', 'vynk.agenda.cache': '{}' })
  borrarDatosDeSesion([local])
  assert.deepEqual(local.claves(), [])
})

test('BORRAR SESIÓN: no toca lo que no es de VYNK', () => {
  const local = almacen({ 'vynk.token': 't', 'tema': 'oscuro', 'otra-app.dato': '1' })
  assert.equal(borrarDatosDeSesion([local]), 1)
  assert.deepEqual(local.claves(), ['tema', 'otra-app.dato'])
})

test('BORRAR SESIÓN: limpia varios almacenes (localStorage y sessionStorage)', () => {
  const local = almacen({ 'vynk.token': 't' })
  const sesion = almacen({ 'vynk.motivoRechazo': '{}' })
  assert.equal(borrarDatosDeSesion([local, sesion]), 2)
  assert.deepEqual([...local.claves(), ...sesion.claves()], [])
})

test('BORRAR SESIÓN: un almacén vacío, ausente o bloqueado no rompe nada', () => {
  const bloqueado = { get length() { throw new Error('SecurityError') } }
  const normal = almacen({ 'vynk.token': 't' })
  assert.equal(borrarDatosDeSesion([almacen({}), null, undefined, bloqueado, normal]), 1)
  assert.deepEqual(normal.claves(), [])
})

test('BORRAR SESIÓN: dos veces seguidas no falla', () => {
  const local = almacen({ 'vynk.token': 't' })
  borrarDatosDeSesion([local])
  assert.equal(borrarDatosDeSesion([local]), 0)
})

test('OTRA PESTAÑA: se detecta que falta el token (la sesión se cerró en otra pestaña)', () => {
  assert.equal(sinTokenGuardado(almacen({ 'vynk.usuario': '{}' })), true)
  assert.equal(sinTokenGuardado(almacen({})), true)
})

test('OTRA PESTAÑA: con token presente no se cierra nada, aunque falte el usuario (login a medias)', () => {
  assert.equal(sinTokenGuardado(almacen({ 'vynk.token': 't' })), false)
  assert.equal(sinTokenGuardado(almacen({ 'vynk.token': 't', 'vynk.usuario': '{}' })), false)
})

test('OTRA PESTAÑA: solo lee, no borra nada', () => {
  const local = almacen({ 'vynk.usuario': '{}', 'vynk.chat': '[]' })
  sinTokenGuardado(local)
  assert.deepEqual(local.claves(), ['vynk.usuario', 'vynk.chat'])
})

test('OTRA PESTAÑA: un almacén bloqueado no cierra la sesión', () => {
  const bloqueado = { getItem() { throw new Error('SecurityError') } }
  assert.equal(sinTokenGuardado(bloqueado), false)
})
