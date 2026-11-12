import test from 'node:test'
import assert from 'node:assert/strict'
import { agruparRondas, avance, calendario, deTipo, frasePorGanador, porDia, puntajeDeLado } from './torneos.js'

const partido = (extra = {}) => ({ id: 'R1-P1', ronda: 1, posicion: 1, nombreRonda: 'Semifinales', a: 'td', b: 'mecanica', estado: 'pendiente', descanso: false, fecha: '', hora: '', lugar: '', ganador: null, resultado: null, ...extra })

test('las rondas salen en orden y los enfrentamientos por posición', () => {
  const r = agruparRondas([partido({ id: 'F', ronda: 2, nombreRonda: 'Final' }), partido({ id: 'B', posicion: 2 }), partido()])
  assert.deepEqual(r.map((x) => x.nombre), ['Semifinales', 'Final'])
  assert.deepEqual(r[0].partidos.map((p) => p.id), ['R1-P1', 'B'])
})

test('el puntaje depende de cómo se mide el torneo', () => {
  assert.equal(puntajeDeLado(partido({ resultado: { modo: 'marcador', a: 3, b: 1 } }), 'a'), 3)
  assert.equal(puntajeDeLado(partido({ resultado: { modo: 'sets', setsA: 2, setsB: 0 } }), 'b'), 0)
  assert.equal(puntajeDeLado(partido({ resultado: { modo: 'ganador' } }), 'a'), null)
  assert.equal(puntajeDeLado(partido(), 'a'), null)
})

test('la frase del ganador usa el nombre del departamento y avisa del desempate', () => {
  assert.equal(frasePorGanador(partido({ ganador: 'quimica', resultado: { modo: 'ganador' } })), 'Ganó Química')
  assert.equal(frasePorGanador(partido({ ganador: 'td', resultado: { modo: 'marcador', a: 1, b: 1, ganadorPenales: 'a' } })), 'Ganó TD en el desempate')
  assert.equal(frasePorGanador(partido()), '')
  assert.equal(frasePorGanador(partido({ descanso: true, ganador: 'td' })), '')
})

test('el calendario solo trae enfrentamientos con horario y rivales, ordenados', () => {
  const t = { partidos: [
    partido({ id: 'c', fecha: '2026-11-18', hora: '10:00', estado: 'programado' }), partido({ id: 'a', fecha: '2026-11-17', hora: '16:00', estado: 'programado' }),
    partido({ id: 'x' }), partido({ id: 'd', descanso: true, fecha: '2026-11-17', hora: '09:00' }), partido({ id: 'e', b: null, fecha: '2026-11-17', hora: '09:00' }),
    partido({ id: 'f', fecha: '2026-11-10', hora: '10:00', estado: 'finalizado' }), partido({ id: 'g', fecha: '2026-11-17', hora: '12:00', estado: 'cancelado' })] }
  assert.deepEqual(calendario(t).map((p) => p.id), ['f', 'a', 'c'])
  assert.deepEqual(calendario(t, { hoyISO: '2026-11-11', soloProximos: true }).map((p) => p.id), ['a', 'c'])
  assert.deepEqual(porDia(calendario(t)).map(([f, ps]) => [f, ps.length]), [['2026-11-10', 1], ['2026-11-17', 1], ['2026-11-18', 1]])
})

test('avance sin descansos y torneos por tipo en orden alfabético', () => {
  assert.deepEqual(avance({ partidos: [partido({ descanso: true, estado: 'finalizado' }), partido({ estado: 'finalizado' }), partido()] }), { jugados: 1, total: 2 })
  assert.deepEqual(deTipo([{ tipo: 'arena', deporte: 'Vóley' }, { tipo: 'karaduo', deporte: 'Karadúo' }, { tipo: 'arena', deporte: 'Fútbol' }], 'arena').map((t) => t.deporte), ['Fútbol', 'Vóley'])
})

import { equipo, normalizarTorneo } from './torneos.js'

const PUBLICO = {
  id: 't1', tipo: 'arena', deporte: 'Fútbol', participantes: ['td', 'td', 'mecanica'],
  equipos: [{ id: 'e1', nombre: 'Los Halcones', area: 'Tecnología Digital', departamento: 'td' }, { id: 'e2', nombre: 'Bits FC', area: 'Tecnología Digital', departamento: 'td' }, { id: 'e3', nombre: 'Mecatrónicos', area: 'Mecánica', departamento: 'mecanica' }],
  campeon: 'td', campeonId: 'e2', subcampeon: 'td', subcampeonId: 'e1',
  partidos: [{ id: 'R1-P1', a: 'td', b: 'td', aId: 'e1', bId: 'e2', ganador: 'td', ganadorId: 'e2', estado: 'finalizado' }],
}

test('el torneo público se convierte a equipos (dos equipos del mismo departamento no se confunden)', () => {
  const t = normalizarTorneo(PUBLICO)
  assert.deepEqual(t.participantes, ['e1', 'e2', 'e3'])
  assert.deepEqual([t.partidos[0].a, t.partidos[0].b, t.partidos[0].ganador], ['e1', 'e2', 'e2'])
  assert.equal(t.campeon, 'e2')
  assert.equal(equipo(t, 'e2').nombre, 'Bits FC')
  assert.equal(equipo(t, 'e3').departamento, 'mecanica')
  assert.equal(frasePorGanador(t.partidos[0], (id) => equipo(t, id).nombre), 'Ganó Bits FC')
})

test('un torneo antiguo (solo departamentos) se sigue viendo igual', () => {
  const viejo = normalizarTorneo({ id: 'x', participantes: ['td', 'dpg'], campeon: 'td', partidos: [{ id: 'R1-P1', a: 'td', b: 'dpg', ganador: 'td' }] })
  assert.deepEqual(viejo.participantes, ['td', 'dpg'])
  assert.equal(equipo(viejo, 'td').nombre, 'TD')
  assert.equal(equipo(viejo, 'td').departamento, 'td')
  assert.equal(equipo(viejo, null), null)
})
