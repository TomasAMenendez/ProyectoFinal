import { describe, expect, it } from 'vitest'
import { calibracionNueva, observarCara, pulsoCalibracion } from './calibracion'

const cara = { mouthSmileLeft: 0.02 }

describe('calibracion de espejito', () => {
  it('termina con cinco caras a lo largo de un segundo y medio', () => {
    let estado = calibracionNueva(0)
    let lista = false
    for (const momento of [100, 400, 800, 1200, 1600]) {
      const paso = observarCara(estado, cara, momento)
      estado = paso.estado
      lista = paso.lista
    }
    expect(lista).toBe(true)
    expect(estado.muestras).toHaveLength(5)
  })

  it('a los ocho segundos de la primera cara usa las muestras que haya', () => {
    let estado = calibracionNueva(0)
    estado = observarCara(estado, cara, 1000).estado
    estado = observarCara(estado, cara, 1200).estado
    const paso = pulsoCalibracion(estado, 9000)
    expect(paso.lista).toBe(true)
    expect(paso.referencia?.mouthSmileLeft).toBeCloseTo(0.02)
  })

  it('a los ocho segundos sin ninguna cara no abre el desafio', () => {
    const paso = pulsoCalibracion(calibracionNueva(0), 8000)
    expect(paso.lista).toBe(false)
    expect(paso.referencia).toBeUndefined()
  })
})
