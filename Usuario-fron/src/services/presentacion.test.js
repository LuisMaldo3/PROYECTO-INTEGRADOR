import assert from 'node:assert/strict'
import { test } from 'node:test'
import { agruparPorDia, bloqueDia, cantidad, estadoCupos, estiloCategoria, hoyISO, nombreDia, partesDeFecha, rangoHoras } from './presentacion.js'

test('DÍA: nombre con día de la semana, sin desfase de zona horaria', () => {
  assert.equal(nombreDia('2026-10-19'), 'Lunes 19 de octubre')
  assert.equal(nombreDia('2026-10-23'), 'Viernes 23 de octubre')
  assert.equal(nombreDia('2027-01-01'), 'Viernes 1 de enero')
})

test('DÍA: sin fecha o inválida dice "Fecha por confirmar"', () => {
  for (const mala of [null, undefined, '', 'hoy', '2026-02-30', '19/10/2026']) assert.equal(nombreDia(mala), 'Fecha por confirmar', String(mala))
  assert.equal(partesDeFecha('2026-13-01'), null)
})

test('DÍA: el bloque grande de la agenda', () => {
  assert.deepEqual(bloqueDia('2026-10-19'), { numero: '19', diaSemana: 'lun', mes: 'oct' })
  assert.equal(bloqueDia('nada'), null)
})

test('HORAS: rango, solo inicio y sin horas', () => {
  assert.equal(rangoHoras('11:00', '12:30'), '11:00 – 12:30')
  assert.equal(rangoHoras('11:00', null), '11:00')
  assert.equal(rangoHoras(null, null), 'Hora por confirmar')
  assert.equal(rangoHoras('', ''), 'Hora por confirmar')
})

test('HOY: la fecha local en formato AAAA-MM-DD', () => {
  assert.equal(hoyISO(new Date(2026, 9, 5, 23, 59)), '2026-10-05')
  assert.equal(hoyISO(new Date(2026, 0, 1, 0, 0)), '2026-01-01')
})

test('AGRUPAR: por día, con las horas en orden y lo que no tiene fecha al final', () => {
  const grupos = agruparPorDia([
    { id: 'a', fecha: '2026-10-20', horaInicio: '15:00' },
    { id: 'b', fecha: '2026-10-19', horaInicio: '15:30' },
    { id: 'c', fecha: null, horaInicio: '09:00' },
    { id: 'd', fecha: '2026-10-19', horaInicio: '09:00' },
    { id: 'e', fecha: 'rara', horaInicio: null },
  ])
  assert.deepEqual(grupos.map((g) => g.fecha), ['2026-10-19', '2026-10-20', null])
  assert.deepEqual(grupos[0].items.map((i) => i.id), ['d', 'b'])
  assert.deepEqual(grupos[2].items.map((i) => i.id).sort(), ['c', 'e'])
  assert.deepEqual(agruparPorDia([]), [])
})

test('CUPOS: hay, quedan pocos, agotado o no se sabe', () => {
  assert.deepEqual(estadoCupos(50), { texto: '50 cupos', tono: 'ok' })
  assert.deepEqual(estadoCupos(6), { texto: '6 cupos', tono: 'ok' })
  assert.deepEqual(estadoCupos(5), { texto: 'Quedan 5 cupos', tono: 'poco' })
  assert.deepEqual(estadoCupos(1), { texto: 'Queda 1 cupo', tono: 'poco' })
  assert.deepEqual(estadoCupos(0), { texto: 'Sin cupos', tono: 'agotado' })
  assert.deepEqual(estadoCupos(null), { texto: 'Cupos por confirmar', tono: 'neutro' })
  assert.deepEqual(estadoCupos(undefined), { texto: 'Cupos por confirmar', tono: 'neutro' })
})

test('CATEGORÍA: se reconoce por id o por nombre', () => {
  assert.equal(estiloCategoria({ id: 'cultural' }).clase, 'cultural')
  assert.equal(estiloCategoria({ id: 'concurso' }).icono, 'trofeo')
  assert.equal(estiloCategoria({ nombre: 'Actividad cultural' }).clase, 'cultural')
  assert.equal(estiloCategoria({ nombre: 'Concurso T&C Week' }).clase, 'concurso')
  assert.equal(estiloCategoria({ nombre: 'CONCURSOS' }).clase, 'concurso')
})

test('CATEGORÍA: lo desconocido sale neutro y conserva su nombre', () => {
  assert.deepEqual(estiloCategoria({ id: 'otra', nombre: 'Taller' }), { clase: 'otra', icono: 'etiqueta', nombre: 'Taller' })
  assert.equal(estiloCategoria({}).nombre, 'Actividad')
  assert.equal(estiloCategoria().clase, 'otra')
  assert.equal(estiloCategoria({ id: 'constructor' }).clase, 'otra')
})

test('CANTIDAD: singular y plural', () => {
  assert.equal(cantidad(1, 'actividad', 'actividades'), '1 actividad')
  assert.equal(cantidad(0, 'actividad', 'actividades'), '0 actividades')
})
