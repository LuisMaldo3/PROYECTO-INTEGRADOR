import assert from 'node:assert/strict'
import { test } from 'node:test'
import { mensajeDeEstado, filtrarUsuarios } from './usuarios.js'

test('búsqueda local filtra rol sin eliminar cuentas válidas', () => {
  assert.deepEqual(filtrarUsuarios([
    { id: '1', rol: 'organizador', nombre: 'Ana' },
    { id: '2', rol: 'administrador', nombre: 'Ana' },
  ], '', 'organizador').map((u) => u.id), ['1'])
})

test('denegación por permiso muestra respuesta de Django', () => {
  assert.equal(mensajeDeEstado(403, 'No tienes permiso para realizar esta acción.'), 'No tienes permiso para realizar esta acción.')
})
