import test from 'node:test'
import assert from 'node:assert/strict'
import { datosEdicionActividad } from './datosEdicionActividad.js'

test('precarga valores existentes para editar', () => {
  const a = datosEdicionActividad({ nombre: 'Taller', categoria: {id:'cat'}, ubicacion: {id:'aud'}, organizadorId:'org', maximoParticipantes: 30 })
  assert.equal(a.nombre, 'Taller')
  assert.equal(a.categoriaId, 'cat')
  assert.equal(a.ubicacionId, 'aud')
  assert.equal(a.organizadorId, 'org')
  assert.equal(a.maximoParticipantes, '30')
})
