import test from 'node:test'
import assert from 'node:assert/strict'
import { guardarDisponibilidad, interpretarHorario, obtenerDisponibilidad, obtenerSugerenciasHuecos, revisarBloques } from './disponibilidadService.js'

test('valida rango semanal y horarios', () => {
  assert.equal(revisarBloques([{ dia: 1, inicio: '08:00', fin: '12:00' }]), '')
  assert.match(revisarBloques([{ dia: 1, inicio: '12:00', fin: '09:00' }]), /posterior/)
  assert.match(revisarBloques([{ dia: 8, inicio: '08:00', fin: '10:00' }]), /día válido/)
  assert.match(revisarBloques([{ dia: 2, inicio: '25:00', fin: '26:00' }]), /formato/)
})
test('carga horario autenticado del usuario', async (t) => {
  const previo = globalThis.fetch
  t.after(() => { globalThis.fetch = previo })
  globalThis.fetch = async (url, opts) => {
    assert.match(url, /api\/disponibilidad$/)
    assert.equal(opts.headers.Authorization, 'Bearer TOKEN')
    return { ok: true, json: async () => ({ confirmado: true, bloques: [] }) }
  }
  assert.equal((await obtenerDisponibilidad('TOKEN')).confirmado, true)
})
test('guarda cambios confirmados mediante PUT', async (t) => {
  const previo = globalThis.fetch
  t.after(() => { globalThis.fetch = previo })
  globalThis.fetch = async (_url, opts) => {
    assert.equal(opts.method, 'PUT')
    assert.deepEqual(JSON.parse(opts.body).bloques, [{ dia: 3, inicio: '09:00', fin: '11:00', nombre: 'Clase' }])
    return { ok: true, json: async () => ({ confirmado: true }) }
  }
  assert.equal((await guardarDisponibilidad('TOKEN', [{ dia: 3, inicio: '09:00', fin: '11:00', nombre: 'Clase' }])).confirmado, true)
})
test('envía imagen como multipart sin exponer token en el cuerpo', async (t) => {
  const previo = globalThis.fetch
  t.after(() => { globalThis.fetch = previo })
  globalThis.fetch = async (_url, opts) => {
    assert.equal(opts.method, 'POST')
    assert.equal(opts.headers.Authorization, 'Bearer TOKEN')
    assert.ok(opts.body instanceof FormData)
    assert.equal(opts.body.get('imagen').name, 'horario.png')
    return { ok: true, json: async () => ({ bloques: [], advertencias: [] }) }
  }
  const archivo = new File(['archivo'], 'horario.png', { type: 'image/png' })
  assert.deepEqual((await interpretarHorario('TOKEN', archivo)).bloques, [])
})
test('consulta sugerencias para huecos y distingue horario no confirmado', async (t) => {
  const previo = globalThis.fetch
  t.after(() => { globalThis.fetch = previo })
  globalThis.fetch = async (url) => {
    assert.match(url, /api\/agenda\/huecos$/)
    return { ok: true, json: async () => ({ configurado: false, actividades: [] }) }
  }
  assert.equal((await obtenerSugerenciasHuecos('TOKEN')).configurado, false)
})
