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
  assert.deepEqual(Object.keys(erroresVisibles(aplicar({ tipo: 'tocarTodo' }))).sort(), ['categoriaId', 'descripcion', 'fecha', 'horaInicio', 'nombre', 'ubicacionId'])
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
    { tipo: 'cambiar', campo: 'ubicacionId', valor: 'auditorio' },
    { tipo: 'cambiar', campo: 'fecha', valor: '2026-10-19' },
    { tipo: 'cambiar', campo: 'horaInicio', valor: '11:00' },
  )
  assert.equal(puedeEnviar(e), true) // la hora de fin es opcional
})

test('FORMULARIO: el error del servidor se muestra y se borra al editar ese campo', () => {
  const lleno = aplicar(
    { tipo: 'cambiar', campo: 'nombre', valor: 'Torneo' },
    { tipo: 'cambiar', campo: 'descripcion', valor: 'Campeonato abierto.' },
    { tipo: 'cambiar', campo: 'categoriaId', valor: 'borrada' },
    { tipo: 'cambiar', campo: 'ubicacionId', valor: 'auditorio' },
    { tipo: 'cambiar', campo: 'fecha', valor: '2026-10-19' },
    { tipo: 'cambiar', campo: 'horaInicio', valor: '11:00' },
  )
  const rechazado = reducir(lleno, { tipo: 'servidor', errores: { categoriaId: 'Elige una categoría válida.' } })
  assert.deepEqual(erroresVisibles(rechazado), { categoriaId: 'Elige una categoría válida.' })
  const corregido = reducir(rechazado, { tipo: 'cambiar', campo: 'categoriaId', valor: 'deportes' })
  assert.deepEqual(erroresVisibles(corregido), {})
})

test('FORMULARIO: reiniciar deja todo como al abrir', () => {
  assert.deepEqual(aplicar({ tipo: 'cambiar', campo: 'nombre', valor: 'x' }, { tipo: 'reiniciar' }), ESTADO_INICIAL)
})

test('FORMULARIO: el error de la hora de fin sale al tocarla y se va al corregirla', () => {
  const e = aplicar(
    { tipo: 'cambiar', campo: 'horaInicio', valor: '15:00' },
    { tipo: 'cambiar', campo: 'horaFin', valor: '14:00' },
  )
  assert.equal(erroresVisibles(e).horaFin, undefined)
  const tocado = reducir(e, { tipo: 'tocar', campo: 'horaFin' })
  assert.equal(erroresVisibles(tocado).horaFin, 'La hora de fin debe ser después de la de inicio.')
  assert.equal(erroresVisibles(reducir(tocado, { tipo: 'cambiar', campo: 'horaFin', valor: '16:00' })).horaFin, undefined)
})

test('FORMULARIO: un error del servidor en la fecha se muestra tal cual', () => {
  const e = reducir(ESTADO_INICIAL, { tipo: 'servidor', errores: { fecha: 'La fecha no es válida.' } })
  assert.equal(erroresVisibles(e).fecha, 'La fecha no es válida.')
})

test('FORMULARIO: la ubicación no se regaña antes de tocarla y su error del servidor se muestra', () => {
  assert.equal(erroresVisibles(ESTADO_INICIAL).ubicacionId, undefined)
  assert.equal(erroresVisibles(reducir(ESTADO_INICIAL, { tipo: 'tocar', campo: 'ubicacionId' })).ubicacionId, 'Elige una ubicación.')
  const rechazado = reducir(ESTADO_INICIAL, { tipo: 'servidor', errores: { ubicacionId: 'Elige una ubicación válida.' } })
  assert.equal(erroresVisibles(rechazado).ubicacionId, 'Elige una ubicación válida.')
  assert.equal(erroresVisibles(reducir(rechazado, { tipo: 'cambiar', campo: 'ubicacionId', valor: 'auditorio' })).ubicacionId, undefined)
})
