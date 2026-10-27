import { describe, expect, it } from 'vitest'
import { actualizarAvance, avanceInicial, confirmado } from './estabilidad'

describe('estabilidad de espejito', () => {
  it('un solo paso no completa el segundo', () => {
    const anclado = actualizarAvance(avanceInicial(), true, 1000)
    const unPaso = actualizarAvance(anclado, true, 2000)
    expect(unPaso.progreso).toBeCloseTo(0.25)
    expect(confirmado(unPaso)).toBe(false)
  })

  it('un segundo efectivo completa el avance', () => {
    let avance = avanceInicial()
    for (let marca = 250; marca <= 1250; marca += 250) {
      avance = actualizarAvance(avance, true, marca)
    }
    expect(avance.progreso).toBeCloseTo(1)
    expect(confirmado(avance)).toBe(true)
  })

  it('soltar la expresion baja el avance sin borrarlo de un golpe', () => {
    let avance = avanceInicial()
    for (let marca = 250; marca <= 1250; marca += 250) {
      avance = actualizarAvance(avance, true, marca)
    }
    const suelto = actualizarAvance(avance, false, 1500)
    expect(suelto.progreso).toBeCloseTo(0.875)
    expect(suelto.progreso).toBeGreaterThan(0)
  })
})
