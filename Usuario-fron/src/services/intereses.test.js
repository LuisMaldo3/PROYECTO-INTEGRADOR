import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  MINIMO_SUGERIDO,
  alternar,
  iconoDeInteres,
  mismosIntereses,
  ordenarComoCatalogo,
  puedeGuardar,
  textoAyuda,
  textoContador,
} from './intereses.js'

const CATALOGO = [{ id: 'teatro' }, { id: 'musica' }, { id: 'tecnologia' }, { id: 'deportes' }]

test('SELECCIÓN: alternar marca y desmarca sin modificar la lista original', () => {
  const inicial = ['teatro']
  const marcado = alternar(inicial, 'musica')
  assert.deepEqual(marcado, ['teatro', 'musica'])
  assert.deepEqual(inicial, ['teatro'])
  assert.deepEqual(alternar(marcado, 'teatro'), ['musica'])
  assert.deepEqual(alternar([], 'deportes'), ['deportes'])
})

test('SELECCIÓN: marcar y desmarcar lo mismo deja todo como estaba', () => {
  assert.deepEqual(alternar(alternar(['teatro'], 'musica'), 'musica'), ['teatro'])
})

test('CAMBIOS: dos listas con los mismos intereses son iguales aunque cambie el orden', () => {
  assert.equal(mismosIntereses(['a', 'b'], ['b', 'a']), true)
  assert.equal(mismosIntereses(['a'], ['a', 'b']), false)
  assert.equal(mismosIntereses(['a', 'b'], ['a', 'c']), false)
  assert.equal(mismosIntereses([], []), true)
})

test('GUARDAR: solo se puede si hay al menos uno, algo cambió y no se está guardando', () => {
  assert.equal(puedeGuardar({ seleccion: ['teatro'], guardados: [] }), true)
  assert.equal(puedeGuardar({ seleccion: [], guardados: ['teatro'] }), false) // sin ninguno no se guarda
  assert.equal(puedeGuardar({ seleccion: ['teatro'], guardados: ['teatro'] }), false) // nada cambió
  assert.equal(puedeGuardar({ seleccion: ['musica', 'teatro'], guardados: ['teatro', 'musica'] }), false) // solo cambió el orden
  assert.equal(puedeGuardar({ seleccion: ['teatro'], guardados: [], guardando: true }), false)
})

test('CATÁLOGO: lo guardado se ordena como el catálogo y se descartan los desconocidos', () => {
  assert.deepEqual(ordenarComoCatalogo(['deportes', 'teatro', 'astrologia'], CATALOGO), ['teatro', 'deportes'])
  assert.deepEqual(ordenarComoCatalogo([], CATALOGO), [])
})

test('ÍCONOS: cada interés del backend tiene el suyo y los nuevos salen con una estrella', () => {
  const ids = ['teatro', 'musica', 'danza', 'libros', 'tecnologia', 'ia', 'emprendimiento', 'videojuegos', 'deportes', 'gastronomia', 'concursos']
  const iconos = ids.map(iconoDeInteres)
  assert.equal(new Set(iconos).size, ids.length) // ninguno repetido
  assert.ok(!iconos.includes('estrella'))
  assert.equal(iconoDeInteres('nuevo'), 'estrella')
  assert.equal(iconoDeInteres('constructor'), 'estrella')
  assert.equal(iconoDeInteres(undefined), 'estrella')
})

test('TEXTOS: el contador y la ayuda cambian según cuántos hay elegidos', () => {
  assert.equal(textoContador(0), 'Elige al menos uno')
  assert.equal(textoContador(1), '1 interés elegido')
  assert.equal(textoContador(4), '4 intereses elegidos')
  assert.match(textoAyuda(0), /Marca los temas/)
  assert.match(textoAyuda(MINIMO_SUGERIDO - 1), /salen mejor/)
  assert.match(textoAyuda(MINIMO_SUGERIDO), /Listo/)
})
