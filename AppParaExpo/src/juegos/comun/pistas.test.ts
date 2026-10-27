import { describe, expect, it } from 'vitest'
import { pistaParaFallo, pistasOrdenadas } from './pistas'

describe('pistas', () => {
  it('ordena primera, tercera y segunda', () => {
    expect(pistasOrdenadas(['una', 'dos', 'tres'])).toEqual(['una', 'tres', 'dos'])
  })

  it('salta la tercera cuando la ficha solo tiene dos', () => {
    expect(pistasOrdenadas(['una', 'dos'])).toEqual(['una', 'dos'])
    expect(pistaParaFallo(['una', 'dos'], 0)).toBe('una')
    expect(pistaParaFallo(['una', 'dos'], 1)).toBe('dos')
    expect(pistaParaFallo(['una', 'dos'], 2)).toBe('dos')
  })
})
