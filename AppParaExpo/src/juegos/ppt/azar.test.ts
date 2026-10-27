import { describe, expect, it } from 'vitest'
import { elegirComputadora, jugarSiLista } from './azar'

describe('azar de la computadora', () => {
  it('no elige hasta que la persona tiene una figura', () => {
    let llamadas = 0
    const azar = () => {
      llamadas += 1
      return 0
    }
    expect(jugarSiLista(null, azar)).toBeNull()
    expect(llamadas).toBe(0)
  })

  it('puede devolver las tres figuras con un generador fijo', () => {
    expect(elegirComputadora(() => 0)).toBe('piedra')
    expect(elegirComputadora(() => 0.34)).toBe('papel')
    expect(elegirComputadora(() => 0.67)).toBe('tijera')
    const cierre = jugarSiLista('piedra', () => 0.67)
    expect(cierre).toEqual({ persona: 'piedra', computadora: 'tijera', resultado: 'gano' })
  })
})
