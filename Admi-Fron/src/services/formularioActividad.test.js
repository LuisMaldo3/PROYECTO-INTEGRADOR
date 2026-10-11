import assert from 'node:assert/strict'
import { test } from 'node:test'
import { ESTADO_INICIAL, erroresVisibles, puedeEnviar, reducir } from './formularioActividad.js'

const aplicar = (...acciones) => acciones.reduce(reducir, ESTADO_INICIAL)

test('FORMULARIO: al abrir no se muestra ningún error', () => {
  assert.deepEqual(erroresVisibles(ESTADO_INICIAL), {})
  assert.equal(puedeEnviar(ESTADO_INICIAL), false)
})

test('FORMULARIO: el error de un campo solo aparece cuando se toca', () => {
  const e = aplicar({ tipo: 'cambiar', campo: 'nombre', valor: 'ab' })
  assert.deepEqual(erroresVisibles(e), {})
  assert.deepEqual(Object.keys(erroresVisibles(reducir(e, { tipo: 'tocar', campo: 'nombre' }))), ['nombre'])
})

test('FORMULARIO: tocarTodo muestra los errores de todos los campos', () => {
  assert.deepEqual(Object.keys(erroresVisibles(aplicar({ tipo: 'tocarTodo' }))).sort(), ['categoriaId', 'descripcion', 'nombre'])
})

test('FORMULARIO: el error desaparece al corregir el campo', () => {
  const e = aplicar(
    { tipo: 'tocar', campo: 'nombre' },
    { tipo: 'cambiar', campo: 'nombre', valor: 'Torneo' },
  )
  assert.equal(erroresVisibles(e).nombre, undefined)
})

test('FORMULARIO: puede enviarse solo con todo correcto', () => {
  const e = aplicar(
    { tipo: 'cambiar', campo: 'nombre', valor: 'Torneo' },
    { tipo: 'cambiar', campo: 'descripcion', valor: 'Campeonato abierto.' },
    { tipo: 'cambiar', campo: 'categoriaId', valor: 'deportes' },
  )
  assert.equal(puedeEnviar(e), true)
})

test('FORMULARIO: el error del servidor se muestra y se borra al editar ese campo', () => {
  const lleno = aplicar(
    { tipo: 'cambiar', campo: 'nombre', valor: 'Torneo' },
    { tipo: 'cambiar', campo: 'descripcion', valor: 'Campeonato abierto.' },
    { tipo: 'cambiar', campo: 'categoriaId', valor: 'borrada' },
  )
  const rechazado = reducir(lleno, { tipo: 'servidor', errores: { categoriaId: 'Elige una categoría válida.' } })
  assert.deepEqual(erroresVisibles(rechazado), { categoriaId: 'Elige una categoría válida.' })
  const corregido = reducir(rechazado, { tipo: 'cambiar', campo: 'categoriaId', valor: 'deportes' })
  assert.deepEqual(erroresVisibles(corregido), {})
})

test('FORMULARIO: reiniciar deja todo como al abrir', () => {
  assert.deepEqual(aplicar({ tipo: 'cambiar', campo: 'nombre', valor: 'x' }, { tipo: 'reiniciar' }), ESTADO_INICIAL)
})
