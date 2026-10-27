import { describe, expect, it } from 'vitest'
import { resultadoDe } from './reglas'

describe('reglas de piedra papel o tijera', () => {
  it('resuelve las nueve parejas', () => {
    expect(resultadoDe('piedra', 'tijera')).toBe('gano')
    expect(resultadoDe('piedra', 'papel')).toBe('perdio')
    expect(resultadoDe('piedra', 'piedra')).toBe('empato')
    expect(resultadoDe('papel', 'piedra')).toBe('gano')
    expect(resultadoDe('papel', 'tijera')).toBe('perdio')
    expect(resultadoDe('papel', 'papel')).toBe('empato')
    expect(resultadoDe('tijera', 'papel')).toBe('gano')
    expect(resultadoDe('tijera', 'piedra')).toBe('perdio')
    expect(resultadoDe('tijera', 'tijera')).toBe('empato')
  })
})
