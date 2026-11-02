import assert from 'node:assert/strict'
import { test } from 'node:test'
import { armarCuerpo } from './actividades.js'
import { validarActividad } from './validacionActividad.js'
const base = { nombre:'Torneo T&C Week', descripcion:'Competencia institucional', categoriaId:'deporte', ubicacionId:'local', organizadorId:'org', fecha:'2026-11-08', horaInicio:'11:00', maximoParticipantes:'12' }
test('US-28: al crear una competencia por equipos se envían sus reglas y configuración', () => {
 const r = armarCuerpo({...base,tipoActividad:'competencia',modalidad:'equipos',maxIntegrantesEquipo:'5',reglasCompetencia:'Reglas generales de este torneo.',puntosPorPuesto:'20, 10, 5'})
 assert.equal(r.tipoActividad,'competencia');assert.equal(r.modalidad,'equipos')
 assert.equal(r.maxIntegrantesEquipo,5);assert.deepEqual(r.puntosPorPuesto,[20,10,5])
})
test('US-31: una competencia por equipos exige límite de integrantes', () => {
 const e = validarActividad({...base,tipoActividad:'competencia',modalidad:'equipos',maxIntegrantesEquipo:'',reglasCompetencia:'Reglas generales de este torneo.'})
 assert.ok(e.maxIntegrantesEquipo)
})
test('US-28: no aplica reglas de competencia a una actividad regular', () => {
 const r = armarCuerpo({...base,tipoActividad:'actividad',reglasCompetencia:'texto de prueba'})
 assert.equal(r.modalidad,null);assert.equal(r.reglasCompetencia,'')
})
