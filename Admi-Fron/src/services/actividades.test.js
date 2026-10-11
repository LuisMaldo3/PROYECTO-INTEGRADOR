import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarCuerpo } from './actividades.js'

test('CUERPO: quita espacios y deja solo los tres campos', () => {
  assert.deepEqual(armarCuerpo({ nombre: '  Torneo ', descripcion: ' Abierto a todos. ', categoriaId: 'deportes', extra: 1 }), {
    nombre: 'Torneo',
    descripcion: 'Abierto a todos.',
    categoriaId: 'deportes',
  })
})

test('CUERPO: lo que falta o no es texto queda vacío', () => {
  assert.deepEqual(armarCuerpo(), { nombre: '', descripcion: '', categoriaId: '' })
  assert.deepEqual(armarCuerpo({ nombre: 5, descripcion: null }), { nombre: '', descripcion: '', categoriaId: '' })
})
