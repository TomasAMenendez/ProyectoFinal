import { describe, expect, it } from 'vitest'
import { cumple, puntuaciones } from './clasificacion'

describe('clasificacion de espejito', () => {
  it('cuenta una sonrisa clara por encima de la cara tranquila', () => {
    const puntos = puntuaciones(
      { mouthSmileLeft: 0.4, mouthSmileRight: 0.4 },
      { mouthSmileLeft: 0.02, mouthSmileRight: 0.02 },
    )
    expect(puntos.feliz).toBeCloseTo(0.38)
    expect(cumple('feliz', puntos)).toBe(true)
  })

  it('no acepta una sonrisa de 0,17 aunque sea la mas alta', () => {
    const puntos = puntuaciones({ mouthSmileLeft: 0.17, mouthSmileRight: 0.17 })
    expect(puntos.feliz).toBeCloseTo(0.17)
    expect(cumple('feliz', puntos)).toBe(false)
  })

  it('no acepta feliz si no llega al 85 por ciento de la mayor', () => {
    const puntos = puntuaciones({
      mouthSmileLeft: 0.2,
      mouthSmileRight: 0.2,
      mouthFrownLeft: 0.3,
      mouthFrownRight: 0.3,
      browInnerUp: 0.3,
    })
    expect(puntos.feliz).toBeCloseTo(0.2)
    expect(puntos.triste).toBeCloseTo(0.3)
    expect(cumple('feliz', puntos)).toBe(false)
    expect(cumple('triste', puntos)).toBe(true)
  })

  it('la cara triste mira la boca y no las cejas', () => {
    const soloCejas = puntuaciones({ browInnerUp: 0.9 })
    expect(soloCejas.triste).toBe(0)
    expect(cumple('triste', soloCejas)).toBe(false)
    const boca = puntuaciones({ mouthFrownLeft: 0.2, mouthFrownRight: 0.2 })
    expect(boca.triste).toBeCloseTo(0.2)
    expect(cumple('triste', boca)).toBe(true)
  })

  it('acepta un puchero chico aunque las cejas bajen como enojado', () => {
    const puntos = puntuaciones({
      mouthFrownLeft: 0.07,
      mouthFrownRight: 0.07,
      browDownLeft: 0.5,
      browDownRight: 0.5,
    })
    expect(puntos.triste).toBeCloseTo(0.07)
    expect(puntos.enojado).toBeGreaterThan(puntos.triste)
    expect(cumple('triste', puntos)).toBe(true)
  })

  it('acepta el labio encogido aunque las comisuras casi no bajen', () => {
    const puntos = puntuaciones({ mouthShrugLower: 0.1 })
    expect(puntos.triste).toBeCloseTo(0.1)
    expect(cumple('triste', puntos)).toBe(true)
  })
})
