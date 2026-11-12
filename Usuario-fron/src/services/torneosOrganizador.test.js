import test from 'node:test'
import assert from 'node:assert/strict'
import { cuerpoResultado, esCorreccion, formularioDeResultado, mensajeDeError, normalizarTorneo, puedeProgramar, puedeRegistrar, transicionesDisponibles } from './torneosOrganizador.js'

const crudo = { id: 't1', tipo: 'arena', deporte: 'Fútbol', modoResultado: 'marcador', estado: 'en_juego', participantes: [{ id: 'td' }, { id: 'mecanica' }], campeon: null, subcampeon: null,
  partidos: [{ id: 'R1-P1', ronda: 1, posicion: 1, nombreRonda: 'Final', a: { id: 'td', nombre: 'TD' }, b: { id: 'mecanica', nombre: 'Mecánica' }, estado: 'finalizado', descanso: false, fecha: '2026-11-17', hora: '15:00', lugar: 'C1', ganador: 'td', validado: false, resultado: { modo: 'marcador', a: 2, b: 1, observaciones: 'ok', registradoEn: 'x' } }] }

test('la respuesta del organizador se normaliza a la forma de la vista de estudiantes', () => {
  const t = normalizarTorneo(crudo)
  assert.deepEqual(t.participantes, ['td', 'mecanica'])
  assert.equal(t.partidos[0].a, 'td')
  assert.equal(t.partidos[0].provisional, true)
  assert.deepEqual(t.partidos[0].resultado, { modo: 'marcador', a: 2, b: 1, setsA: undefined, setsB: undefined, ganadorPenales: undefined, observaciones: 'ok' })
  assert.equal(normalizarTorneo({ ...crudo, partidos: [{ ...crudo.partidos[0], validado: true }] }).partidos[0].provisional, false)
  assert.equal(normalizarTorneo({ id: 'x' }).partidos.length, 0)
})

test('qué puede hacer el organizador con cada enfrentamiento', () => {
  const p = normalizarTorneo(crudo).partidos[0]
  assert.ok(puedeRegistrar(p) && esCorreccion(p) && !puedeProgramar(p))
  const abierto = { ...p, estado: 'programado', resultado: null }
  assert.ok(puedeProgramar(abierto) && puedeRegistrar(abierto) && !esCorreccion(abierto))
  assert.ok(!puedeRegistrar({ ...abierto, b: null }) && !puedeRegistrar({ ...abierto, descanso: true }) && !puedeRegistrar({ ...abierto, estado: 'cancelado' }))
  assert.deepEqual(transicionesDisponibles(abierto), ['en_curso', 'suspendido', 'cancelado'])
  assert.deepEqual(transicionesDisponibles({ ...abierto, estado: 'suspendido', fecha: '', hora: '' }), ['pendiente', 'cancelado'])
})

test('el resultado se arma según cómo se mide el torneo', () => {
  assert.deepEqual(cuerpoResultado('marcador', { a: '2', b: '1', observaciones: ' x ' }), { a: '2', b: '1', observaciones: 'x' })
  assert.equal(cuerpoResultado('marcador', { a: '1', b: '1', ganadorPenales: 'b', observaciones: '' }).ganadorPenales, 'b')
  assert.equal('ganadorPenales' in cuerpoResultado('marcador', { a: '2', b: '1', ganadorPenales: 'b', observaciones: '' }), false)
  assert.deepEqual(cuerpoResultado('sets', { setsA: 2, setsB: 0, observaciones: '' }), { setsA: 2, setsB: 0, observaciones: '' })
  assert.deepEqual(cuerpoResultado('ganador', { ganador: 'a', observaciones: '' }), { ganador: 'a', observaciones: '' })
  assert.equal(formularioDeResultado(normalizarTorneo(crudo).partidos[0]).ganador, 'a')
})

test('el mensaje de error prefiere el detalle del servidor', () => {
  assert.equal(mensajeDeError({ errores: { hora: 'TD ya juega a esa hora.' } }), 'TD ya juega a esa hora.')
  assert.equal(mensajeDeError({ errores: { a: 'x', b: 'y' } }), 'Revisa los datos marcados.')
  assert.equal(mensajeDeError(new Error('Sin conexión')), 'Sin conexión')
})

test('el torneo normalizado conserva su competencia y los datos de cada equipo', () => {
  const t = normalizarTorneo({ id: 't9', actividadId: 'act-1', participantes: [{ id: 'e1', nombre: 'Rayos', area: 'Mecánica', departamento: 'mecanica' }], partidos: [] })
  assert.equal(t.actividadId, 'act-1')
  assert.equal(t.equipos.e1.nombre, 'Rayos')
})
