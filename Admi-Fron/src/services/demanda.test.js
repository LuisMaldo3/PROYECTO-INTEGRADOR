import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizarDemanda, filtrarDemanda } from './demanda.js'

test('US-37: conserva el ranking devuelto por el backend y las unidades de cupo', () => {
  const datos = normalizarDemanda({ actividades: [
    { id: '1', nombre: 'Robótica', posicion: 1, unidadCupo: 'equipos', ocupados: 2, capacidad: 3, personasInscritas: 7, porcentajeOcupacion: 66.7 },
    { id: '2', nombre: 'Taller', posicion: 2, unidadCupo: 'estudiantes', ocupados: 5, capacidad: 10, porcentajeOcupacion: 50 },
  ] })
  assert.deepEqual(datos.actividades.map((a) => a.id), ['1', '2'])
  assert.equal(datos.actividades[0].unidadCupo, 'equipos')
  assert.equal(datos.actividades[0].personasInscritas, 7)
  assert.equal(datos.actividades[0].porcentajeOcupacion, 66.7)
})

test('US-37: filtra competencias y búsqueda por área sin modificar posiciones', () => {
  const a = normalizarDemanda({ actividades: [
    { id: 'r', nombre: 'Robótica', posicion: 1, tipoActividad: 'competencia', areaTecsup: 'Tecnología Digital' },
    { id: 'm', nombre: 'Taller mecánico', posicion: 2, tipoActividad: 'actividad', areaTecsup: 'Mecánica' },
  ] }).actividades
  assert.equal(filtrarDemanda(a, { tipo: 'competencias' }).length, 1)
  assert.equal(filtrarDemanda(a, { consulta: 'MECÁNICA' })[0].id, 'm')
  assert.equal(filtrarDemanda(a, { tipo: 'actividades' })[0].posicion, 2)
})

test('US-37: sin cupo definido no inventa porcentaje de ocupación', () => {
  const a = normalizarDemanda({ actividades: [{ id: 'a', ocupados: 4, capacidad: null, porcentajeOcupacion: null }] }).actividades[0]
  assert.equal(a.capacidad, null)
  assert.equal(a.porcentajeOcupacion, null)
})

test('US-37: sin IA configurada no presenta una explicación ficticia', () => {
  const data = normalizarDemanda({ ia: { estado: 'sin_configurar', texto: null, mensaje: 'Sin clave' } })
  assert.equal(data.ia.estado, 'sin_configurar')
  assert.equal(data.ia.texto, null)
})
