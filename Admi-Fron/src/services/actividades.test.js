import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarCuerpo } from './actividades.js'

test('CUERPO: quita espacios y deja solo los campos de la actividad', () => {
  assert.deepEqual(
    armarCuerpo({
      nombre: '  Torneo ',
      descripcion: ' Abierto a todos. ',
      categoriaId: 'actividad-cultural',
      fecha: '2026-10-19',
      horaInicio: '11:00',
      horaFin: ' 14:30 ',
      extra: 1,
    }),
    {
      nombre: 'Torneo',
      descripcion: 'Abierto a todos.',
      categoriaId: 'actividad-cultural',
      fecha: '2026-10-19',
      horaInicio: '11:00',
      horaFin: '14:30',
    },
  )
})

test('CUERPO: sin hora de fin se envía vacía', () => {
  assert.equal(armarCuerpo({ nombre: 'x', horaInicio: '15:30' }).horaFin, '')
})

test('CUERPO: lo que falta o no es texto queda vacío', () => {
  const vacio = { nombre: '', descripcion: '', categoriaId: '', fecha: '', horaInicio: '', horaFin: '' }
  assert.deepEqual(armarCuerpo(), vacio)
  assert.deepEqual(armarCuerpo({ nombre: 5, descripcion: null, fecha: 20261019 }), vacio)
})
