import test from 'node:test'
import assert from 'node:assert/strict'
import { DEPARTAMENTOS, claveDepartamento, departamentoDeArea, departamentoPorId, nombreDepartamento, rutaEscudo } from './departamentos.js'
import { AREAS_TECSUP, claveArea } from './areas.js'

test('los cinco departamentos tienen el color definido para el proyecto', () => {
  const colores = Object.fromEntries(DEPARTAMENTOS.map((d) => [d.id, d.color]))
  assert.deepEqual(colores, { quimica: 'verde', mecanica: 'amarillo', td: 'rojo', electricidad: 'azul', dpg: 'morado' })
  assert.equal(DEPARTAMENTOS.length, 5)
})

test('cada área que ya existe en los estudiantes pertenece a un departamento, sin repetirse', () => {
  const ids = AREAS_TECSUP.map((a) => departamentoDeArea(a)?.id)
  assert.ok(ids.every(Boolean), 'toda área tiene departamento')
  assert.equal(new Set(ids).size, 5)
  assert.equal(departamentoDeArea('Tecnología Digital').id, 'td')
  assert.equal(departamentoDeArea('Carrera inventada'), null)
})

test('el color del CSS coincide con el del departamento', () => {
  for (const d of DEPARTAMENTOS) assert.equal(claveArea(d.area), d.clave, d.id)
  assert.equal(claveDepartamento('td'), 'digital')
  assert.equal(claveDepartamento('nada'), 'general')
})

test('solo DPG está por confirmar y los datos básicos se resuelven', () => {
  assert.deepEqual(DEPARTAMENTOS.filter((d) => d.porConfirmar).map((d) => d.id), ['dpg'])
  assert.equal(nombreDepartamento('mecanica'), 'Mecánica')
  assert.equal(nombreDepartamento(null), 'Por definir')
  assert.equal(departamentoPorId('quimica').hex, '#12A150')
  assert.equal(rutaEscudo('td'), '/escudos/td.svg')
})
