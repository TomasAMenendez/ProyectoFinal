import { describe, expect, it } from 'vitest'
import { crearAdivina } from '../adivina/partida'
import { crearContar } from '../contar/partida'
import { crearSombras } from '../sombras/partida'
import { mulberry32 } from './azar'
import { volverAlMenu } from './visita'
import type { EjercicioContar, FichaAdivina, FichaSombra } from './tipos'

describe('reinicio', () => {
  it('restaura vidas y cambia el desafio', () => {
    const sombras: FichaSombra[] = Array.from({ length: 8 }, (_, indice) => ({
      id: `s-${indice}`,
      nombreVisible: `nombre-${indice}`,
      pistas: ['primera', 'segunda'],
      silueta: `${indice}.png`,
    }))
    const primera = crearSombras(sombras, null, mulberry32(5))
    const segunda = crearSombras(sombras, primera.fichas.map((ficha) => ficha.id), mulberry32(5))
    expect(segunda.vidas).toBe(3)
    expect(segunda.uniones).toEqual([])
    expect(segunda.fichas.map((ficha) => ficha.id).sort().join()).not.toBe(primera.fichas.map((ficha) => ficha.id).sort().join())

    const objetos: FichaAdivina[] = Array.from({ length: 6 }, (_, indice) => ({
      id: `a-${indice}`,
      nombreCorrecto: `objeto-${indice}`,
      pistas: ['primera', 'segunda', 'tercera'],
      imagen: `${indice}.png`,
    }))
    const adivina = crearAdivina(objetos, null, mulberry32(6))
    expect(crearAdivina(objetos, adivina.objetivo.id, mulberry32(6)).objetivo.id).not.toBe(adivina.objetivo.id)

    const conteos: EjercicioContar[] = [
      { id: 'uno', imagen: 'uno.png', pregunta: '¿Cuántos hay?', cantidad: 1 },
      { id: 'dos', imagen: 'dos.png', pregunta: '¿Cuántos hay ahora?', cantidad: 2 },
    ]
    const conteo = crearContar(conteos, null, () => 0)
    expect(crearContar(conteos, conteo.ejercicio.id, () => 0.9).ejercicio.id).not.toBe(conteo.ejercicio.id)
  })

  it('al volver al menu no conserva la pantalla del juego', () => {
    expect(volverAlMenu({ nombre: 'Luz', pantalla: 'contar' })).toEqual({ nombre: 'Luz', pantalla: 'menu' })
  })
})
