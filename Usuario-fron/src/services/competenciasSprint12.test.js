import assert from 'node:assert/strict'
import { test } from 'node:test'
import { PANTALLAS, resolverRuta, pantallasDelMenu } from './permisos.js'
test('US-07: estudiante y organizador pueden consultar resultados', () => {
 assert.equal(resolverRuta('#/resultados','usuario').estado,'permitida')
 assert.equal(resolverRuta('#/resultados','organizador').estado,'permitida')
 assert.ok(pantallasDelMenu('usuario').find(x=>x.ruta==='resultados'))
})
test('US-47: el detalle del organizador sigue protegido', () => {
 assert.equal(resolverRuta('#/organizador-detalle','usuario').estado,'bloqueada')
 assert.equal(resolverRuta('#/organizador-detalle','organizador').estado,'permitida')
})
