import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizarResumenEvento, filtrarResultados } from './resumenEvento.js'

test('US-39 normaliza totales sin inventar datos', () => {
  const informe = normalizarResumenEvento({})
  assert.equal(informe.totales.puntosOficiales, 0)
  assert.equal(informe.resultados.length, 0)
  assert.equal(informe.ia.estado, 'no_solicitada')
})

test('US-39 conserva cinco áreas y sus puntajes oficiales', () => {
  const reporte = normalizarResumenEvento({
    totales: { competenciasConResultados: 2, puntosOficiales: 35 },
    areas: [
      { area: 'Mecánica', puntos: 20, primerosPuestos: 1 },
      { area: 'Tecnología Digital', puntos: 15, primerosPuestos: 1 },
      { area: 'Electricidad y Electrónica', puntos: 0 },
      { area: 'Minería y Procesos Químicos', puntos: 0 },
      { area: 'Gestión y Producción', puntos: 0 },
    ],
  })
  assert.equal(reporte.totales.puntosOficiales, 35)
  assert.equal(reporte.areas.length, 5)
  assert.equal(reporte.areas[0].area, 'Mecánica')
})

test('US-39 muestra solo resultados enviados por la API, sin inventar ganadores', () => {
  const reporte = normalizarResumenEvento({resultados: [{actividadId: 'a', nombre: 'Robótica', puntosDistribuidos: 10, posiciones: []}]})
  assert.deepEqual(reporte.resultados[0].ganadores, [])
  assert.equal(reporte.resultados[0].puntosDistribuidos, 10)
})

test('US-39 permite filtrar por nombre de competencia, participante y área', () => {
  const resultados = [{ nombre: 'Robótica', posiciones: [{nombre: 'Luis', areaTecsup: 'Tecnología Digital'}] },
    { nombre: 'Mecánica', posiciones: [{nombre: 'Otro', areaTecsup: 'Mecánica'}] }]
  assert.equal(filtrarResultados(resultados, 'robó').length, 1)
  assert.equal(filtrarResultados(resultados, 'Luis').length, 1)
  assert.equal(filtrarResultados(resultados, 'Tecnología Digital').length, 1)
  assert.equal(filtrarResultados(resultados, '').length, 2)
})

test('US-39 separa explicación de IA de datos oficiales', () => {
  const reporte = normalizarResumenEvento({ia: {estado: 'no_disponible', mensaje: 'Sin Gemini'}, totales: {puntosOficiales: 8}})
  assert.equal(reporte.ia.estado, 'no_disponible')
  assert.equal(reporte.totales.puntosOficiales, 8)
})
