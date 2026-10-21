import { test } from 'node:test'
import assert from 'node:assert/strict'
import { PREGUNTAS_NATURALES } from './preguntasNaturales.js'

test('chat web: incluye ejemplos de tema y horario comprensibles', () => {
  assert.ok(PREGUNTAS_NATURALES.some(({ texto }) => /deportes/i.test(texto) && /después de las 3/i.test(texto)))
  assert.ok(PREGUNTAS_NATURALES.some(({ texto }) => /programación/i.test(texto) && /mañana/i.test(texto)))
  assert.ok(PREGUNTAS_NATURALES.every(({ texto, icono }) => texto.length <= 600 && icono))
})
