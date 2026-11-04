import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizarIntereses, filtrarIntereses } from './intereses.js'

test('US-38: conserva conteos reales, etiquetas y porcentaje de estudiantes', () => {
  const d = normalizarIntereses({
    totales: { estudiantesActivos: 12, conIntereses: 10, sinIntereses: 2, selecciones: 15, categorias: 2 },
    intereses: [
      { id: 'musica', nombre: 'Música', estudiantes: 8, porcentaje: 80, posicion: 1 },
      { id: 'ia', nombre: 'Inteligencia artificial', estudiantes: 7, porcentaje: 70, posicion: 2 },
    ],
  })
  assert.equal(d.totales.conIntereses, 10)
  assert.equal(d.totales.selecciones, 15)
  assert.deepEqual(d.intereses.map((i) => i.estudiantes), [8, 7])
  assert.deepEqual(d.intereses.map((i) => i.porcentaje), [80, 70])
})

test('US-38: sin registros no inventa valores ni explicación de IA', () => {
  const d = normalizarIntereses({ ia: { estado: 'sin_datos', texto: null, mensaje: 'Sin selecciones' } })
  assert.equal(d.totales.estudiantesActivos, 0)
  assert.deepEqual(d.intereses, [])
  assert.equal(d.ia.texto, null)
})

test('US-38: filtra con acentos y por categorías realmente seleccionadas', () => {
  const lista = [
    { nombre: 'Tecnología e innovación', estudiantes: 4 },
    { nombre: 'Música', estudiantes: 0 },
  ]
  assert.equal(filtrarIntereses(lista, 'TECNOLOGÍA').length, 1)
  assert.equal(filtrarIntereses(lista, '', true).length, 1)
  assert.equal(filtrarIntereses(lista, 'no existe').length, 0)
})

test('US-38: ignora números negativos y porcentajes mayores de cien', () => {
  const d = normalizarIntereses({ intereses: [{ estudiantes: -2, porcentaje: 150 }] })
  assert.equal(d.intereses[0].estudiantes, 0)
  assert.equal(d.intereses[0].porcentaje, 100)
})
