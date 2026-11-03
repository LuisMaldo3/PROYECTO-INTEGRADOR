import test from 'node:test'
import assert from 'node:assert/strict'
import { describirInscritos, filtrarActividades, marcasEje, maximoEje, normalizarActividad, normalizarRespuesta, ordenarActividades } from './inscritos.js'

const taller = normalizarActividad({ id: 't', nombre: 'Taller', personasInscritas: 8, capacidad: 10, unidadCupo: 'estudiantes', fecha: '2026-11-20' })
const futbol = normalizarActividad({ id: 'f', nombre: 'Fútbol', tipoActividad: 'competencia', unidadCupo: 'equipos', personasInscritas: 12, equiposRegistrados: 4, equiposCompletos: 3, capacidad: 8, fecha: '2026-11-18' })
const charla = normalizarActividad({ id: 'c', nombre: 'Charla', personasInscritas: 0, fecha: '2026-11-25' })
const feria = normalizarActividad({ id: 'x', nombre: 'Feria', personasInscritas: 3, estado: 'cancelada' })

test('US-35 · normaliza conteos: faltantes en 0, sin negativos y ocupación en %', () => {
  const a = normalizarActividad({ id: 1, personasInscritas: -3, equiposRegistrados: 2, equiposCompletos: 9, unidadCupo: 'equipos', capacidad: 4 })
  assert.equal(a.personasInscritas, 0)
  assert.equal(a.equiposCompletos, 2)      // nunca más completos que registrados
  assert.equal(a.equiposIncompletos, 0)
  assert.equal(a.ocupados, 2)              // por equipos, el cupo se cuenta por equipo
  assert.equal(a.porcentaje, 50)
  assert.equal(taller.porcentaje, 80)
  assert.equal(charla.porcentaje, null)    // sin límite de cupos
})

test('US-35-T05 · distingue personas de equipos completos', () => {
  assert.equal(futbol.equiposIncompletos, 1)
  assert.equal(describirInscritos(futbol), '12 personas · 3 de 4 equipos completos')
  assert.equal(describirInscritos(taller), '8 personas')
  assert.equal(describirInscritos(normalizarActividad({ personasInscritas: 1, unidadCupo: 'equipos' })), '1 persona · sin equipos')
})

test('US-35 · filtra, busca y oculta canceladas', () => {
  const todas = [taller, futbol, charla, feria]
  assert.deepEqual(filtrarActividades(todas).map((a) => a.id), ['t', 'f', 'c'])
  assert.deepEqual(filtrarActividades(todas, { filtro: 'equipos' }).map((a) => a.id), ['f'])
  assert.deepEqual(filtrarActividades(todas, { filtro: 'sin-inscritos' }).map((a) => a.id), ['c'])
  assert.deepEqual(filtrarActividades(todas, { busqueda: 'fút' }).map((a) => a.id), ['f'])
  assert.equal(filtrarActividades(todas, { incluirCanceladas: true }).length, 4)
})

test('US-35 · ordena por inscritos, ocupación o fecha sin mutar la lista', () => {
  const lista = [charla, taller, futbol]
  assert.deepEqual(ordenarActividades(lista).map((a) => a.id), ['f', 't', 'c'])
  assert.deepEqual(ordenarActividades(lista, 'ocupacion').map((a) => a.id), ['t', 'f', 'c'])
  assert.deepEqual(ordenarActividades(lista, 'fecha').map((a) => a.id), ['f', 't', 'c'])
  assert.deepEqual(lista.map((a) => a.id), ['c', 't', 'f'])
})

test('US-35 · el eje del gráfico usa máximos redondos', () => {
  assert.equal(maximoEje([0, 2]), 5)
  assert.equal(maximoEje([7]), 10)
  assert.equal(maximoEje([23, 4]), 25)
  assert.equal(maximoEje([140]), 200)
  assert.deepEqual(marcasEje(20), [0, 5, 10, 15, 20])
})

test('US-35 · respuesta vacía o mal formada no rompe la pantalla', () => {
  const r = normalizarRespuesta(null)
  assert.deepEqual(r.actividades, [])
  assert.equal(r.totales.personasInscritas, 0)
})
