import test from 'node:test'
import assert from 'node:assert/strict'
import { cuerpoHoja, etiquetaCategoria, idPrueba, totalFila } from './puntajes.js'

test('el id de una prueba sale del nombre y no se repite', () => {
  assert.equal(idPrueba('Carrera de sacos'), 'carrera-de-sacos')
  assert.equal(idPrueba('Acertijo número 1'), 'acertijo-numero-1')
  assert.equal(idPrueba('Carrera', ['carrera']), 'carrera-2')
  assert.equal(idPrueba('¡¡!!'), 'prueba')
})

test('la hoja se envía sin casillas vacías y con números', () => {
  const pruebas = [{ id: 'a', nombre: ' Carrera ' }, { id: 'b', nombre: 'Acertijo' }]
  assert.deepEqual(cuerpoHoja(pruebas, { e1: { a: '10', b: '' }, e2: { a: '', b: '' }, e3: { z: '5' } }, 3),
    { pruebas: [{ id: 'a', nombre: 'Carrera' }, { id: 'b', nombre: 'Acertijo' }], puntajes: { e1: { a: 10 } }, version: 3 })
})

test('el total en pantalla suma las pruebas', () => {
  assert.equal(totalFila({ a: '10', b: 5.5 }, [{ id: 'a' }, { id: 'b' }]), 15.5)
  assert.equal(totalFila(undefined, [{ id: 'a' }]), 0)
})

test('la categoría se muestra con su nombre', () => {
  assert.equal(etiquetaCategoria('damas'), 'Damas')
  assert.equal(etiquetaCategoria(''), '')
})
