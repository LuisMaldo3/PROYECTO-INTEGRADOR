import assert from 'node:assert/strict'
import { test } from 'node:test'
import { esActividadTCWeek, obtenerSemanaEspecial, filtrarPorEtapa } from './campanas.js'

test('T&C Week se detecta exclusivamente cuando está identificada', () => {
 assert.equal(esActividadTCWeek({ nombre: 'T&C Week · Innovación' }), true)
 assert.equal(esActividadTCWeek({ nombre: 'Concurso de fotografía' }), false)
 assert.equal(esActividadTCWeek({ nombre: 'Tech and Culture Week' }), true)
})
test('El evento no se promociona después de terminar ni si supera una semana', () => {
 const a = [{ nombre: 'T&C Week', fecha: '2026-11-02' }, { nombre: 'T&C Week: cierre', fecha: '2026-11-06' }]
 assert.deepEqual(obtenerSemanaEspecial(a, '2026-11-03'), { inicio: '2026-11-02', fin: '2026-11-06', cantidad: 2 })
 assert.equal(obtenerSemanaEspecial(a, '2026-11-07'), null)
 assert.equal(obtenerSemanaEspecial([{ nombre:'Tec Week', fecha:'2026-11-01' }, { nombre:'Tec Week', fecha:'2026-11-20' }], '2026-11-03'), null)
 assert.equal(obtenerSemanaEspecial([{ nombre:'Debate académico', fecha:'2026-11-03' }], '2026-11-03'), null)
 assert.deepEqual(obtenerSemanaEspecial([{ nombre:'T&C Week', fecha:'2025-11-01' }, ...a], '2026-11-03'), { inicio: '2026-11-02', fin: '2026-11-06', cantidad: 2 })
})
test('Actividades posteriores siguen visibles: próximos, históricos y todos', () => {
 const a = [{ fecha:'2026-10-20' }, { fecha:'2026-11-03' }, { fecha:'2026-11-20' }, { fecha:null }]
 assert.equal(filtrarPorEtapa(a, 'proximas', '2026-11-03').length, 3)
 assert.equal(filtrarPorEtapa(a, 'pasadas', '2026-11-03').length, 1)
 assert.equal(filtrarPorEtapa(a, 'todas', '2026-11-03').length, 4)
})
