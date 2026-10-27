import { describe, expect, it } from 'vitest'
import { indiceEn, planDeLectura, ventanaDe } from './lectura'

const cuento = 'Había una vez un patito. Nadie quería jugar.\n\nAl final encontró amigos.'

describe('lectura de cuentos', () => {
  it('parte el cuento en oraciones y no lo deja en un solo bloque', () => {
    const plan = planDeLectura(cuento)
    expect(plan.map((fragmento) => fragmento.texto)).toEqual([
      'Había una vez un patito.',
      'Nadie quería jugar.',
      'Al final encontró amigos.',
    ])
  })

  it('avanza la oración según el tiempo del audio', () => {
    const plan = planDeLectura(cuento)
    expect(indiceEn(plan, 0)).toBe(0)
    expect(indiceEn(plan, 1)).toBe(2)
    const medio = indiceEn(plan, 0.5)
    expect(medio).toBeGreaterThan(0)
    expect(medio).toBeLessThan(plan.length)
  })

  it('muestra solo un tramo alrededor de la oración actual', () => {
    const plan = planDeLectura(cuento)
    const ventana = ventanaDe(plan, 2)
    expect(ventana).toHaveLength(2)
    expect(ventana.map((trozo) => trozo.texto).join(' ')).not.toContain('Había una vez')
    expect(ventana[1]).toMatchObject({ activo: true, texto: 'Al final encontró amigos.' })
  })
})
