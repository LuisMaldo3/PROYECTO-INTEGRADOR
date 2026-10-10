import assert from 'node:assert/strict'
import { test } from 'node:test'
import { etiquetaEstado, etiquetaRol, filtrarUsuarios, mensajeDeEstado } from './usuarios.js'

const LISTA = [
  { id: '1', nombre: 'Ángela Ríos', correo: 'angela@tecsup.edu.pe', rol: 'administrador', activo: true },
  { id: '2', nombre: 'Beto Paz', correo: 'beto@tecsup.edu.pe', rol: 'usuario', activo: true },
  { id: '3', nombre: 'Caro Díaz', correo: 'caro@tecsup.edu.pe', rol: 'organizador', activo: false },
]

test('ROL: etiquetas legibles y lo desconocido cuenta como Usuario', () => {
  assert.equal(etiquetaRol('administrador'), 'Administrador')
  assert.equal(etiquetaRol('ORGANIZADOR'), 'Organizador')
  assert.equal(etiquetaRol('otro'), 'Usuario')
  assert.equal(etiquetaRol(undefined), 'Usuario')
})

test('ESTADO: solo activo === false es Inactivo', () => {
  assert.equal(etiquetaEstado(true), 'Activo')
  assert.equal(etiquetaEstado(false), 'Inactivo')
  assert.equal(etiquetaEstado(undefined), 'Activo')
})

test('FILTRO: sin texto ni rol devuelve todos', () => {
  assert.equal(filtrarUsuarios(LISTA).length, 3)
})

test('FILTRO: busca por nombre o correo, sin tildes ni mayúsculas', () => {
  assert.deepEqual(filtrarUsuarios(LISTA, 'angela').map((u) => u.id), ['1'])
  assert.deepEqual(filtrarUsuarios(LISTA, 'RIOS').map((u) => u.id), ['1'])
  assert.deepEqual(filtrarUsuarios(LISTA, 'caro@').map((u) => u.id), ['3'])
  assert.deepEqual(filtrarUsuarios(LISTA, 'nadie'), [])
})

test('FILTRO: por rol y combinado con texto', () => {
  assert.deepEqual(filtrarUsuarios(LISTA, '', 'usuario').map((u) => u.id), ['2'])
  assert.deepEqual(filtrarUsuarios(LISTA, 'caro', 'organizador').map((u) => u.id), ['3'])
  assert.deepEqual(filtrarUsuarios(LISTA, 'caro', 'usuario'), [])
})

test('MENSAJES: según el estado HTTP', () => {
  assert.match(mensajeDeEstado(401), /sesión expiró/)
  assert.match(mensajeDeEstado(403), /administradores/)
  assert.match(mensajeDeEstado(404), /No se encontró/)
  assert.equal(mensajeDeEstado(500, 'Falla X'), 'Falla X')
  assert.match(mensajeDeEstado(500), /No se pudo/)
})
