import assert from 'node:assert/strict'
import { test } from 'node:test'
import { claveHistorial, leerHistorial, guardarHistorial, borrarHistorial } from './historialAsistente.js'

test('Chat: historial aislado por cuenta, sin credenciales', () => {
  const datos = new Map()
  globalThis.localStorage = {
    getItem: k => datos.get(k) ?? null,
    setItem: (k, v) => datos.set(k, v),
    removeItem: k => datos.delete(k),
  }
  assert.notEqual(claveHistorial('ana@tecsup.edu.pe'), claveHistorial('juan@tecsup.edu.pe'))
  guardarHistorial('ana@tecsup.edu.pe', [{ rol: 'usuario', contenido: '¿Qué hay hoy?', token: 'no-guardar' }])
  assert.equal(leerHistorial('juan@tecsup.edu.pe').length, 0)
  assert.equal(leerHistorial('ana@tecsup.edu.pe')[0].contenido, '¿Qué hay hoy?')
  assert.equal(JSON.stringify(leerHistorial('ana@tecsup.edu.pe')).includes('no-guardar'), false)
  borrarHistorial('ana@tecsup.edu.pe')
  assert.deepEqual(leerHistorial('ana@tecsup.edu.pe'), [])
  delete globalThis.localStorage
})

test('Asistente: conserva las tarjetas de ubicación al reabrir el historial', async () => {
  const { guardarHistorial, leerHistorial } = await import('./historialAsistente.js')
  const previo = globalThis.localStorage
  const contenido = new Map()
  globalThis.localStorage = { setItem: (k,v) => contenido.set(k,v), getItem: k => contenido.get(k) ?? null }
  try {
    guardarHistorial('alumno@tecsup.edu.pe', [{ rol: 'asistente', contenido: 'El lugar es el auditorio', ubicaciones: [{ actividadId: '1', lugar: 'Auditorio' }] }])
    assert.equal(leerHistorial('alumno@tecsup.edu.pe')[0].ubicaciones[0].lugar, 'Auditorio')
  } finally { globalThis.localStorage = previo }
})


test('Historial: conserva requisitos registrados en el mensaje del asistente', () => {
  const previo = globalThis.localStorage
  const contenido = new Map()
  globalThis.localStorage = { setItem: (k,v) => contenido.set(k,v), getItem: k => contenido.get(k) ?? null, removeItem: k => contenido.delete(k) }
  try {
    const correo = 'requisitos@tecsup.edu.pe'
    guardarHistorial(correo, [{ rol: 'asistente', contenido: 'Requisitos de robótica', tipoConsulta: 'requisitos', requisitos: [{ actividadId: '1', nombre: 'Robótica', items: ['Laptop'], confirmados: true }] }])
    const recuperado = leerHistorial(correo)
    assert.equal(recuperado[0].requisitos[0].items[0], 'Laptop')
    borrarHistorial(correo)
  } finally { globalThis.localStorage = previo }
})
