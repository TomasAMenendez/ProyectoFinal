import { describe, expect, it } from 'vitest'
import { esArrastre, palabraBajoDedo, segmentoHastaDedo } from './arrastre'

describe('arrastre de sombras', () => {
  it('un movimiento corto sigue siendo un toque', () => {
    expect(esArrastre({ x: 10, y: 10 }, { x: 16, y: 14 })).toBe(false)
  })

  it('sigue el dedo con una linea desde la silueta', () => {
    expect(esArrastre({ x: 10, y: 10 }, { x: 40, y: 12 })).toBe(true)
    expect(segmentoHastaDedo({ x: 20, y: 30 }, { x: 180, y: 90 }, { left: 8, top: 40 })).toEqual({
      id: 'temporal',
      x1: 20,
      y1: 30,
      x2: 172,
      y2: 50,
    })
  })

  it('suelta sobre la palabra que esta bajo el dedo', () => {
    const palabras = [
      { id: 'pato', left: 200, top: 10, right: 280, bottom: 70, resuelto: false },
      { id: 'sol', left: 200, top: 80, right: 280, bottom: 140, resuelto: true },
    ]
    expect(palabraBajoDedo(240, 40, palabras)).toBe('pato')
    expect(palabraBajoDedo(240, 100, palabras)).toBeNull()
    expect(palabraBajoDedo(10, 40, palabras)).toBeNull()
  })
})
