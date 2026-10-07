import assert from 'node:assert/strict'
import { test } from 'node:test'
import { borrarMotivo, guardarMotivo, leerMotivo } from './motivoRechazo.js'

/** Almacenamiento de mentira, igual que sessionStorage pero en memoria. */
function almacenFalso(inicial = {}) {
  const datos = new Map(Object.entries(inicial))
  return {
    getItem: (k) => (datos.has(k) ? datos.get(k) : null),
    setItem: (k, v) => void datos.set(k, String(v)),
    removeItem: (k) => void datos.delete(k),
    _datos: datos,
  }
}

const ahora = Date.parse('2026-10-12T15:30:00Z')

test('guarda el motivo del rechazo y se puede leer después', () => {
  const almacen = almacenFalso()
  const guardado = guardarMotivo({ codigo: 'DOMINIO_NO_PERMITIDO', mensaje: 'Usa tu correo @tecsup.edu.pe' }, almacen, ahora)
  assert.deepEqual(guardado, { codigo: 'DOMINIO_NO_PERMITIDO', mensaje: 'Usa tu correo @tecsup.edu.pe', fecha: '2026-10-12T15:30:00.000Z' })
  assert.deepEqual(leerMotivo(almacen), guardado)
})

test('solo guarda código, mensaje y fecha (nunca correo ni contraseña)', () => {
  const almacen = almacenFalso()
  guardarMotivo({ codigo: 'LOGIN_INCORRECTO', mensaje: 'Correo o contraseña incorrectos.', correo: 'ana@gmail.com', password: 'secreto' }, almacen, ahora)
  const texto = [...almacen._datos.values()].join('')
  assert.deepEqual(Object.keys(JSON.parse(texto)).sort(), ['codigo', 'fecha', 'mensaje'])
  assert.equal(texto.includes('ana@gmail.com'), false)
  assert.equal(texto.includes('secreto'), false)
})

test('un nuevo rechazo reemplaza al anterior', () => {
  const almacen = almacenFalso()
  guardarMotivo({ codigo: 'A', mensaje: 'primero' }, almacen, ahora)
  guardarMotivo({ codigo: 'B', mensaje: 'segundo' }, almacen, ahora)
  assert.equal(leerMotivo(almacen).mensaje, 'segundo')
})

test('sin código se guarda como ERROR', () => {
  assert.equal(guardarMotivo({ mensaje: 'algo falló' }, almacenFalso(), ahora).codigo, 'ERROR')
})

test('borrarMotivo lo elimina', () => {
  const almacen = almacenFalso()
  guardarMotivo({ codigo: 'A', mensaje: 'motivo' }, almacen, ahora)
  borrarMotivo(almacen)
  assert.equal(leerMotivo(almacen), null)
})

test('no guarda nada si no hay mensaje', () => {
  const almacen = almacenFalso()
  assert.equal(guardarMotivo({ codigo: 'A', mensaje: '' }, almacen, ahora), null)
  assert.equal(leerMotivo(almacen), null)
})

test('si no hay almacenamiento (null) o falla, nada se rompe', () => {
  assert.equal(guardarMotivo({ codigo: 'A', mensaje: 'x' }, null, ahora), null)
  assert.equal(leerMotivo(null), null)
  assert.doesNotThrow(() => borrarMotivo(null))
  const roto = { getItem() { throw new Error('bloqueado') }, setItem() { throw new Error('bloqueado') }, removeItem() { throw new Error('bloqueado') } }
  assert.equal(guardarMotivo({ codigo: 'A', mensaje: 'x' }, roto, ahora), null)
  assert.equal(leerMotivo(roto), null)
  assert.doesNotThrow(() => borrarMotivo(roto))
})

test('un valor dañado en el almacenamiento se ignora', () => {
  assert.equal(leerMotivo(almacenFalso({ 'vynk.motivoRechazo': '{no es json' })), null)
  assert.equal(leerMotivo(almacenFalso({ 'vynk.motivoRechazo': JSON.stringify({ codigo: 'A' }) })), null)
})
