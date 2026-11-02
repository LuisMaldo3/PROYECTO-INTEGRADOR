import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarCuerpo } from './actividades.js'

test('CUERPO: quita espacios y deja solo los campos de la actividad', () => {
  assert.deepEqual(
    armarCuerpo({
      nombre: '  Torneo ',
      descripcion: ' Abierto a todos. ',
      categoriaId: 'actividad-cultural',
      ubicacionId: ' patio-central ',
      organizadorId: ' organizador-1 ',
      maximoParticipantes: '25',
      fecha: '2026-10-19',
      horaInicio: '11:00',
      horaFin: ' 14:30 ',
      extra: 1,
    }),
    {
      nombre: 'Torneo',
      portada: null,
      areaTecsup: '',
      encargadosPorArea: {},
      descripcion: 'Abierto a todos.',
      categoriaId: 'actividad-cultural',
      ubicacionId: 'patio-central',
      organizadorId: 'organizador-1',
      maximoParticipantes: 25,
      fecha: '2026-10-19',
      horaInicio: '11:00',
      horaFin: '14:30',
      tipoActividad: 'actividad', modalidad: null, maxIntegrantesEquipo: null, reglasCompetencia: '', puntosPorPuesto: [],
    },
  )
})

test('CUERPO: sin hora de fin se envía vacía', () => {
  assert.equal(armarCuerpo({ nombre: 'x', horaInicio: '15:30' }).horaFin, '')
})

test('CUERPO: lo que falta o no es texto queda vacío', () => {
  const vacio = { nombre: '', portada: null, areaTecsup: '', encargadosPorArea: {}, descripcion: '', categoriaId: '', ubicacionId: '', organizadorId: '', fecha: '', horaInicio: '', horaFin: '', maximoParticipantes: NaN, tipoActividad: 'actividad', modalidad: null, maxIntegrantesEquipo: null, reglasCompetencia: '', puntosPorPuesto: [] }
  assert.deepEqual(armarCuerpo(), vacio)
  assert.deepEqual(armarCuerpo({ nombre: 5, descripcion: null, fecha: 20261019 }), vacio)
})
