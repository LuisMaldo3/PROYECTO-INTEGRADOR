import test from 'node:test'
import assert from 'node:assert/strict'
import { agruparRondas, cuerpoResultado, descansos, formularioDeResultado, proximosPartidos, puedeProgramar, puedeRegistrar, puedeValidar, puntajeDeLado, resumenTorneo, textoResultado, transicionesDisponibles } from './torneos.js'

const td = { id: 'td', nombre: 'TD' }, mec = { id: 'mecanica', nombre: 'Mecánica' }
const partido = (extra = {}) => ({ id: 'R1-P1', ronda: 1, posicion: 1, nombreRonda: 'Semifinales', a: td, b: mec, estado: 'pendiente', descanso: false, fecha: '', hora: '', lugar: '', resultado: null, ganador: null, validado: false, ...extra })

test('las rondas salen en orden con sus partidos por posición', () => {
  const rondas = agruparRondas([partido({ id: 'R2-P1', ronda: 2, nombreRonda: 'Final' }), partido({ id: 'R1-P2', posicion: 2 }), partido()])
  assert.deepEqual(rondas.map((r) => r.nombre), ['Semifinales', 'Final'])
  assert.deepEqual(rondas[0].partidos.map((p) => p.id), ['R1-P1', 'R1-P2'])
})

test('con cantidad impar descansan los que sobran, sin inventar departamentos', () => {
  assert.deepEqual([2, 3, 4, 5, 6, 7, 8].map(descansos), [0, 1, 0, 3, 2, 1, 0])
  assert.equal(descansos(1), 0)
})

test('texto del resultado según cómo se mide el torneo', () => {
  assert.equal(textoResultado(partido({ resultado: { modo: 'marcador', a: 2, b: 1 }, ganador: 'td' }), 'marcador'), '2 – 1')
  assert.equal(textoResultado(partido({ resultado: { modo: 'marcador', a: 1, b: 1, ganadorPenales: 'b' }, ganador: 'mecanica' }), 'marcador'), '1 – 1 (pen.)')
  assert.equal(textoResultado(partido({ resultado: { modo: 'sets', setsA: 2, setsB: 1 } }), 'sets'), 'Sets 2 – 1')
  assert.equal(textoResultado(partido({ resultado: { modo: 'ganador' }, ganador: 'mecanica' }), 'ganador'), 'Ganó Mecánica')
  assert.equal(textoResultado(partido(), 'marcador'), '')
  assert.equal(puntajeDeLado(partido({ resultado: { modo: 'sets', setsA: 2, setsB: 0 } }), 'b'), 0)
  assert.equal(puntajeDeLado(partido({ resultado: { modo: 'ganador' } }), 'a'), null)
})

test('qué botones se ofrecen según el estado del enfrentamiento', () => {
  assert.ok(puedeProgramar(partido()) && puedeRegistrar(partido()))
  const sinRival = partido({ b: null })
  assert.ok(!puedeProgramar(sinRival) && !puedeRegistrar(sinRival))
  const descanso = partido({ descanso: true, estado: 'finalizado' })
  assert.ok(!puedeProgramar(descanso) && !puedeRegistrar(descanso) && !puedeValidar(descanso))
  const jugado = partido({ estado: 'finalizado', resultado: { modo: 'marcador', a: 1, b: 0 }, ganador: 'td' })
  assert.ok(puedeRegistrar(jugado), 'un resultado se puede corregir')
  assert.ok(!puedeProgramar(jugado) && puedeValidar(jugado))
  assert.ok(!puedeValidar({ ...jugado, validado: true }))
  assert.ok(!puedeRegistrar(partido({ estado: 'cancelado' })) && !puedeRegistrar(partido({ estado: 'suspendido' })))
})

test('transiciones: no se finaliza a mano y un suspendido sin horario vuelve a pendiente', () => {
  assert.deepEqual(transicionesDisponibles(partido({ estado: 'programado' })), ['en_curso', 'suspendido', 'cancelado'])
  assert.ok(!transicionesDisponibles(partido({ estado: 'programado' })).includes('finalizado'))
  assert.deepEqual(transicionesDisponibles(partido({ estado: 'suspendido' })), ['pendiente', 'cancelado'])
  assert.ok(transicionesDisponibles(partido({ estado: 'suspendido', fecha: '2026-11-17', hora: '15:00' })).includes('programado'))
  assert.deepEqual(transicionesDisponibles(partido({ estado: 'finalizado' })), [])
  assert.deepEqual(transicionesDisponibles(partido({ descanso: true })), [])
})

test('el formulario del resultado se convierte en el cuerpo del backend', () => {
  assert.deepEqual(cuerpoResultado('marcador', { a: 2, b: 1, observaciones: ' ok ' }), { a: 2, b: 1, observaciones: 'ok' })
  assert.deepEqual(cuerpoResultado('marcador', { a: '1', b: '1', ganadorPenales: 'a', observaciones: '' }), { a: '1', b: '1', observaciones: '', ganadorPenales: 'a' })
  assert.deepEqual(cuerpoResultado('sets', { setsA: 2, setsB: 0, observaciones: '' }), { setsA: 2, setsB: 0, observaciones: '' })
  assert.deepEqual(cuerpoResultado('ganador', { ganador: 'b', observaciones: '' }), { ganador: 'b', observaciones: '' })
  const f = formularioDeResultado(partido({ resultado: { modo: 'marcador', a: 3, b: 0 }, ganador: 'td' }))
  assert.equal(f.a, 3)
  assert.equal(f.ganador, 'a')
})

test('avance del torneo sin contar descansos y próximos partidos ordenados', () => {
  const t = { id: 't1', deporte: 'Fútbol', tipo: 'arena', estado: 'en_juego', partidos: [partido({ id: 'a', descanso: true, estado: 'finalizado' }), partido({ id: 'b', estado: 'finalizado' }), partido({ id: 'c', fecha: '2026-11-18', hora: '10:00', estado: 'programado' }), partido({ id: 'd', fecha: '2026-11-17', hora: '16:00', estado: 'programado' }), partido({ id: 'e', fecha: '2026-11-10', hora: '10:00', estado: 'programado' })] }
  assert.deepEqual(resumenTorneo(t), { jugados: 1, total: 4, porcentaje: 25 })
  assert.deepEqual(proximosPartidos([t], '2026-11-11').map((p) => p.id), ['d', 'c'])
  assert.equal(proximosPartidos([{ ...t, estado: 'anulado' }], '2026-11-11').length, 0)
})

import { estadoPuestos, puntosDelPuesto, puntosEscritos } from './torneos.js'

test('los puntos de cada puesto salen de lo configurado en la competencia', () => {
  assert.equal(puntosDelPuesto([10, 7, 5, 3, 1], 1), 10)
  assert.equal(puntosDelPuesto([10, 7, 5, 3, 1], 5), 1)
  assert.equal(puntosDelPuesto([10, 7], 3), 0, 'un puesto sin configurar no suma')
  assert.equal(puntosDelPuesto(null, 1), 0)
})

test('el estado de los puestos explica el paso siguiente', () => {
  assert.equal(estadoPuestos({}).tono, 'info')
  assert.equal(estadoPuestos({ posiciones: [{ puesto: 1 }] }).tono, 'aviso')
  assert.equal(estadoPuestos({ posiciones: [{ puesto: 1 }], puestosEnviados: true }).tono, 'aviso')
  assert.equal(estadoPuestos({ posiciones: [{ puesto: 1 }], puestosEnviados: true, resultadosPublicados: true }).tono, 'ok')
})

test('los puntos escritos a mano se validan antes de enviar', () => {
  const pos = [{ puesto: 1 }, { puesto: 2 }]
  assert.deepEqual(puntosEscritos(pos, { 1: '10', 2: 6 }), { 1: 10, 2: 6 })
  assert.equal(puntosEscritos(pos, { 1: '10' }), null, 'falta el 2.º')
  assert.equal(puntosEscritos(pos, { 1: '10', 2: '-1' }), null)
  assert.equal(puntosEscritos(pos, { 1: '10', 2: '2.5' }), null)
  assert.deepEqual(puntosEscritos(pos, { 1: '0', 2: '0' }), { 1: 0, 2: 0 }, 'cero es válido')
})
