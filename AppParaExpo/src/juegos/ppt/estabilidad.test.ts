import { describe, expect, it } from 'vitest'
import { avanzarTramo, iniciarCuenta, vigilarCuenta, type Tramo } from './estabilidad'

const vacio: Tramo = { figura: null, desde: null }

describe('estabilidad del gesto', () => {
  it('no confirma por debajo de 0,7 ni antes de 800 ms', () => {
    const debil = avanzarTramo(vacio, { figura: 'piedra', confianza: 0.69, momento: 0 })
    expect(debil.confirmado).toBe(false)
    expect(debil.tramo.figura).toBeNull()
    const corto = avanzarTramo({ figura: 'piedra', desde: 0 }, { figura: 'piedra', confianza: 0.9, momento: 799 })
    expect(corto.confirmado).toBe(false)
  })

  it('confirma la misma figura al llegar a 800 ms', () => {
    const listo = avanzarTramo({ figura: 'papel', desde: 1000 }, { figura: 'papel', confianza: 0.7, momento: 1800 })
    expect(listo.confirmado).toBe(true)
  })

  it('reinicia el tramo si la figura cambia', () => {
    const cambio = avanzarTramo({ figura: 'piedra', desde: 0 }, { figura: 'tijera', confianza: 0.95, momento: 900 })
    expect(cambio.confirmado).toBe(false)
    expect(cambio.tramo).toEqual({ figura: 'tijera', desde: 900 })
  })

  it('anula la cuenta si el gesto no se sostiene', () => {
    const cuenta = iniciarCuenta('piedra', 0)
    expect(vigilarCuenta(cuenta, { figura: 'papel', confianza: 0.9, momento: 200 }).estado).toBe('cancelada')
    expect(vigilarCuenta(cuenta, { figura: null, confianza: 0, momento: 200 }).estado).toBe('cancelada')
  })

  it('avanza 3, 2 y 1 y termina solo al sostener el mismo gesto', () => {
    const tres = vigilarCuenta(iniciarCuenta('tijera', 0), { figura: 'tijera', confianza: 0.8, momento: 1000 })
    expect(tres.estado).toBe('sigue')
    if (tres.estado !== 'sigue') return
    expect(tres.cuenta.numero).toBe(2)
    const dos = vigilarCuenta(tres.cuenta, { figura: 'tijera', confianza: 0.8, momento: 2000 })
    if (dos.estado !== 'sigue') throw new Error('debio seguir')
    expect(dos.cuenta.numero).toBe(1)
    const uno = vigilarCuenta(dos.cuenta, { figura: 'tijera', confianza: 0.8, momento: 3000 })
    expect(uno.estado).toBe('lista')
  })
})
