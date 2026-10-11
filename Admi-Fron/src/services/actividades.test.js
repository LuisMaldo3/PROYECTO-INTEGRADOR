import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarCuerpo } from './actividades.js'

test('CUERPO: quita espacios y deja solo los seis campos', () => {
  assert.deepEqual(
    armarCuerpo({
      nombre: '  Torneo ',
      descripcion: ' Abierto a todos. ',
      categoriaId: 'deportes',
      fecha: ' 2026-10-19 ',
      horaInicio: '11:00 ',
      horaFin: ' 14:30',
      extra: 1,
    }),
    {
      nombre: 'Torneo',
      descripcion: 'Abierto a todos.',
      categoriaId: 'deportes',
      fecha: '2026-10-19',
      horaInicio: '11:00',
      horaFin: '14:30',
    },
  )
})

test('CUERPO: lo que falta o no es texto queda vacío', () => {
  const vacio = { nombre: '', descripcion: '', categoriaId: '', fecha: '', horaInicio: '', horaFin: '' }
  assert.deepEqual(armarCuerpo(), vacio)
  assert.deepEqual(armarCuerpo({ nombre: 5, descripcion: null }), vacio)
})
