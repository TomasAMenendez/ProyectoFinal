import { describe, expect, it } from 'vitest'
import { VIDAS_INICIALES, corazonQueSeRompe, descontarVida } from './vidas'

describe('vidas', () => {
  it('rompe los corazones de derecha a izquierda', () => {
    expect(VIDAS_INICIALES).toBe(3)
    expect(corazonQueSeRompe(3)).toBe('derecha')
    expect(corazonQueSeRompe(2)).toBe('medio')
    expect(corazonQueSeRompe(1)).toBe('izquierda')
    expect(descontarVida(1)).toBe(0)
    expect(corazonQueSeRompe(0)).toBeNull()
  })
})
