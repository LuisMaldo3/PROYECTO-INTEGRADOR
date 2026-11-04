import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizarComparacion, categoriasComparadas, clasificacionComparada, CINCO_AREAS } from './comparacionCategorias.js'

const fixture = normalizarComparacion({
  totales: { participaciones: 11, puntosOficiales: 25 },
  categorias: [
    { id: 'deportes', nombre: 'Deportes', personasInscritas: 6, equiposCompletos: 1, equiposIncompletos: 1 },
    { id: 'tecnologia', nombre: 'Tecnología', personasInscritas: 5 },
  ],
  clasificacion: { areas: [] },
  competencias: [
    { actividadId: 'a', nombre: 'Fútbol', categoriaId: 'deportes', categoriaNombre: 'Deportes', posiciones: [
      { puesto: 1, areaTecsup: CINCO_AREAS[0], puntos: 20 },
      { puesto: 2, areaTecsup: CINCO_AREAS[1], puntos: 5 },
    ] },
    { actividadId: 'b', nombre: 'Robótica', categoriaId: 'tecnologia', categoriaNombre: 'Tecnología', posiciones: [] },
  ],
})

test('US-40-02: el gráfico conserva datos reales, no inventa visitantes', () => {
  assert.equal(fixture.totales.participaciones, 11)
  assert.equal(fixture.categorias[0].personasInscritas, 6)
  assert.equal(fixture.categorias[0].equiposCompletos, 1)
})

test('US-40-03: selección compara una o varias categorías', () => {
  assert.equal(categoriasComparadas(fixture).length, 2)
  assert.deepEqual(categoriasComparadas(fixture, ['deportes']).map(c => c.nombre), ['Deportes'])
})

test('US-40-05: conserva exactamente las cinco áreas', () => {
  const r = clasificacionComparada(fixture)
  assert.equal(r.areas.length, 5)
  assert.equal(r.puntosTotales, 25)
  assert.equal(r.areas[0].puntos, 20)
})

test('US-40-07: clasificación se filtra por categoría y área', () => {
  assert.equal(clasificacionComparada(fixture, ['tecnologia']).puntosTotales, 0)
  const filtro = clasificacionComparada(fixture, ['deportes'], CINCO_AREAS[1])
  assert.equal(filtro.areas.length, 1)
  assert.equal(filtro.areas[0].puntos, 5)
  assert.equal(filtro.totalCompetencias, 1)
})

test('US-40-08: desglose de puntos incluye competencia y categoría', () => {
  const desglose = clasificacionComparada(fixture).areas[0].desglose[0]
  assert.equal(desglose.competencia, 'Fútbol')
  assert.equal(desglose.categoria, 'Deportes')
  assert.equal(desglose.puntos, 20)
})

test('US-40: sin datos no inventa resultados ni puntos', () => {
  const r = clasificacionComparada(normalizarComparacion({}))
  assert.equal(r.puntosTotales, 0)
  assert.equal(r.areas.length, 5)
  assert.equal(r.totalCompetencias, 0)
})
