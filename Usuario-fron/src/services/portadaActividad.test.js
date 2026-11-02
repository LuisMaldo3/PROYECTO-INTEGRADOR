import test from 'node:test'
import assert from 'node:assert/strict'
import { urlImagenActividad, creditoPortada } from './portadaActividad.js'

test('convierte ruta de portada generada a Spring Boot', () => {
  assert.match(urlImagenActividad({ portadaUrl: '/api/portadas/archivo/123456789012345678901234' }), /api\/portadas\/archivo\/123456789012345678901234/)
})
test('permite solo imágenes Wikimedia en fuentes externas', () => {
  assert.equal(urlImagenActividad({ portadaUrl: 'https://evil.example/test.png' }), '')
  assert.equal(urlImagenActividad({ portadaUrl: 'https://upload.wikimedia.org/example.jpg' }), 'https://upload.wikimedia.org/example.jpg')
})
test('exige enlace Commons para atribución', () => {
  assert.equal(creditoPortada({ portada: { tipo: 'gemini' } }), null)
  assert.equal(creditoPortada({ portada: { tipo: 'commons', creditoUrl: 'http://other.example' } }), null)
})
