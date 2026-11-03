import test from 'node:test'
import assert from 'node:assert/strict'
import { claveArea } from './areas.js'

test('cada área de Tecsup tiene su clave de color', () => {
  assert.equal(claveArea('Tecnología Digital'), 'digital')
  assert.equal(claveArea('Electricidad y Electrónica'), 'electro')
  assert.equal(claveArea('Mecánica'), 'mecanica')
  assert.equal(claveArea('Minería y Procesos Químicos'), 'quimica')
  assert.equal(claveArea('Gestión y Producción'), 'gestion')
  assert.equal(claveArea(''), 'general')
  assert.equal(claveArea(undefined), 'general')
})
