import { describe, expect, it } from 'vitest'
import { sesionNueva, siguiente } from './sesion'

describe('sesion de espejito', () => {
  it('pide una sola emocion por partida', () => {
    expect(sesionNueva(() => 0).orden).toEqual(['feliz'])
    expect(sesionNueva(() => 0.99).orden).toEqual(['sorprendido'])
  })

  it('anota una sola vez al imitar esa cara', () => {
    const primera = siguiente(sesionNueva(() => 0))
    expect(primera.anotar).toBe(true)
    expect(siguiente(primera.sesion).anotar).toBe(false)
  })
})
