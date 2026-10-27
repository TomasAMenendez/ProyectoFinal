import { describe, expect, it } from 'vitest'
import { fraseAcierto, fraseError, fraseMenu, fraseRejuego } from './frases'

describe('variantes', () => {
  it('no repite una sola frase en cinco veces', () => {
    for (const pedir of [fraseAcierto, fraseError, fraseRejuego, fraseMenu]) {
      const dichas = Array.from({ length: 5 }, () => pedir())
      expect(new Set(dichas).size).toBeGreaterThan(1)
    }
  })
})
