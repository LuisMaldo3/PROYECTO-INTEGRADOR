import assert from 'node:assert/strict'
import { test } from 'node:test'
import { formatearFechaActividad, formatearHora, formatearHorario, resumen } from './formatoActividad.js'

test('RESUMEN: el texto corto queda igual y el largo se recorta con …', () => {
  assert.equal(resumen('  Hola  '), 'Hola')
  assert.equal(resumen('a'.repeat(80)), 'a'.repeat(80))
  const largo = resumen('a'.repeat(200), 20)
  assert.equal(largo.length, 20)
  assert.ok(largo.endsWith('…'))
  assert.equal(resumen(undefined), '')
})

test('HORA: pasa de 24 h a 12 h como en la programación', () => {
  assert.equal(formatearHora('11:00'), '11:00 a. m.')
  assert.equal(formatearHora('14:30'), '2:30 p. m.')
  assert.equal(formatearHora('15:30'), '3:30 p. m.')
  assert.equal(formatearHora('00:05'), '12:05 a. m.') // medianoche
  assert.equal(formatearHora('12:00'), '12:00 p. m.') // mediodía
  assert.equal(formatearHora('23:59'), '11:59 p. m.')
})

test('HORA: lo que no es HH:MM da texto vacío', () => {
  for (const mala of ['', null, undefined, '25:00', '9:00', 'abc', 1100]) assert.equal(formatearHora(mala), '', String(mala))
})

test('HORARIO: con inicio y fin, solo inicio, o sin datos', () => {
  assert.equal(formatearHorario('11:00', '14:30'), '11:00 a. m. – 2:30 p. m.')
  assert.equal(formatearHorario('15:30', null), '3:30 p. m.')
  assert.equal(formatearHorario('15:30', ''), '3:30 p. m.')
  assert.equal(formatearHorario('15:30', 'xx'), '3:30 p. m.') // una hora de fin dañada no rompe la lista
  assert.equal(formatearHorario(null, '14:30'), '—')
  assert.equal(formatearHorario(), '—')
})

test('FECHA: muestra el día correcto sin correrse por la zona horaria', () => {
  assert.match(formatearFechaActividad('2026-10-19'), /\b19\b/)
  assert.match(formatearFechaActividad('2026-10-23'), /\b23\b/)
  assert.match(formatearFechaActividad('2026-01-01'), /\b1\b/)
})

test('FECHA: vacía, mal escrita o imposible da «Sin fecha»', () => {
  for (const mala of ['', null, undefined, 'no es fecha', '2026-02-30', '19/10/2026']) {
    assert.equal(formatearFechaActividad(mala), 'Sin fecha', String(mala))
  }
})
