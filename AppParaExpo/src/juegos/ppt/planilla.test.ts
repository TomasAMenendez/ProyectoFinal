import { describe, expect, it } from 'vitest'
import type { JuegoId } from '../comun/tipos'
import { anotaEnPlanilla } from './planilla'

describe('planilla de piedra papel o tijera', () => {
  it('anota este juego igual que los otros tres', () => {
    expect(anotaEnPlanilla('ppt')).toBe(true)
    expect(anotaEnPlanilla('espejito')).toBe(true)
    const anteriores: JuegoId[] = ['sombras', 'adivina', 'contar']
    expect(anteriores.every(anotaEnPlanilla)).toBe(true)
  })
})
