import assert from 'node:assert/strict'
import { test } from 'node:test'
import { descartarRecomendacion, retirarTarjetaDescartada } from './recomendacionesService.js'

const originalFetch = globalThis.fetch
const originalStorage = globalThis.localStorage
const token = `eyJhbGciOiJub25lIn0.${Buffer.from(JSON.stringify({ exp: 4102444800 })).toString('base64url')}.firma`

function conSesion() {
  const datos = new Map([
    ['vynk.token', token],
    ['vynk.usuario', JSON.stringify({ correo: 'estudiante@tecsup.edu.pe', rol: 'usuario', nombre: 'Estudiante' })],
  ])
  globalThis.localStorage = {
    getItem: key => datos.get(key) ?? null,
    removeItem: key => datos.delete(key),
    setItem: (key, value) => datos.set(key, value),
  }
}

function restaurar() {
  globalThis.fetch = originalFetch
  globalThis.localStorage = originalStorage
}

test('descarte web: llama POST con JWT y no envía otros usuarios', async () => {
  conSesion()
  globalThis.fetch = async (url, options) => {
    assert.match(url, /\/api\/recomendaciones\/actividad-1\/descartar$/)
    assert.equal(options.method, 'POST')
    assert.equal(options.headers.Authorization, `Bearer ${token}`)
    assert.equal(options.body, undefined)
    return { status: 204, ok: true }
  }
  try { assert.equal(await descartarRecomendacion('actividad-1'), true) }
  finally { restaurar() }
})

test('descarte web: informa que falta actualizar el backend ante 404', async () => {
  conSesion()
  globalThis.fetch = async () => ({ status: 404, ok: false })
  try { await assert.rejects(descartarRecomendacion('a1'), /backend/) }
  finally { restaurar() }
})

test('descarte web: falla si no existe conexión', async () => {
  conSesion()
  globalThis.fetch = async () => { throw new TypeError('Network failed') }
  try { await assert.rejects(descartarRecomendacion('a1'), /No hay conexión/) }
  finally { restaurar() }
})

test('descarte web: rechaza IDs inválidos antes de llamar a la red', async () => {
  conSesion()
  globalThis.fetch = () => { throw new Error('No debió enviar una petición') }
  try { await assert.rejects(descartarRecomendacion('../a'), /identificador válido/) }
  finally { restaurar() }
})

test('descarte web: solicita volver a iniciar sesión si recibe HTTP 401', async () => {
  conSesion()
  globalThis.fetch = async () => ({ status: 401, ok: false })
  try { await assert.rejects(descartarRecomendacion('a1'), /sesión expiró/) }
  finally { restaurar() }
})

test('descarte web: retira solo la recomendación seleccionada y mantiene el original para revertir', () => {
  const anterior = { actividades: [{ id: 'a1' }, { id: 'a2' }, { id: 'a3' }], origen: 'reglas' }
  const nuevo = retirarTarjetaDescartada(anterior, 'a2')
  assert.deepEqual(nuevo.actividades.map(x => x.id), ['a1', 'a3'])
  assert.equal(anterior.actividades.length, 3)
  assert.equal(nuevo.origen, 'reglas')
  assert.deepEqual(retirarTarjetaDescartada(nuevo, 'a2').actividades, nuevo.actividades)
})
