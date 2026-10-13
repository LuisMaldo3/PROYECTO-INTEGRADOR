import { describe, it, expect } from 'vitest'
import { actividadesEnCruce } from './crucesService'
describe('cruces en la agenda web', () => {
  it('identifica ambas actividades', () => {
    expect([...actividadesEnCruce([{ actividadAId: '1', actividadBId: '2' }])]).toEqual(['1', '2'])
  })
  it('no resalta cuando no hay cruces', () => {
    expect(actividadesEnCruce([]).size).toBe(0)
  })
})
