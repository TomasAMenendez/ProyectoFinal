import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { crearAdivina } from '../adivina/partida'
import { crearContar } from '../contar/partida'
import { crearSombras } from '../sombras/partida'
import { mulberry32 } from './azar'
import { conjuntosIguales, elegirElementos, mezclar } from './sorteo'
import type { EjercicioContar, FichaAdivina, FichaSombra } from './tipos'

function sombra(id: string): FichaSombra {
  return { id, nombreVisible: `nombre-${id}`, pistas: ['primera', 'segunda'], silueta: `${id}.png` }
}

describe('sorteo', () => {
  it('elige cinco sombras distintas y mezcla los nombres', () => {
    const catalogo = Array.from({ length: 8 }, (_, indice) => sombra(String(indice)))
    const partida = crearSombras(catalogo, null, mulberry32(4))
    expect(new Set(partida.fichas.map((ficha) => ficha.id)).size).toBe(5)
    expect(partida.nombres.map((nombre) => nombre.id).join('|')).not.toBe(partida.fichas.map((ficha) => ficha.id).join('|'))
  })

  it('no repite el mismo conjunto al reiniciar', () => {
    const catalogo = Array.from({ length: 8 }, (_, indice) => sombra(String(indice)))
    const primero = elegirElementos(catalogo, 5, (ficha) => ficha.id, null, mulberry32(2)).map((ficha) => ficha.id)
    const segundo = elegirElementos(catalogo, 5, (ficha) => ficha.id, primero, mulberry32(2)).map((ficha) => ficha.id)
    expect(conjuntosIguales(primero, segundo)).toBe(false)
  })

  it('arma cuatro distractores distintos y mueve la respuesta', () => {
    const catalogo: FichaAdivina[] = Array.from({ length: 7 }, (_, indice) => ({
      id: `obj-${indice}`,
      nombreCorrecto: `objeto-${indice}`,
      pistas: ['primera', 'segunda', 'tercera'],
      imagen: `${indice}.png`,
    }))
    const posiciones = new Set<number>()
    for (let semilla = 1; semilla <= 20; semilla += 1) {
      const partida = crearAdivina(catalogo, null, mulberry32(semilla))
      expect(partida.opciones).toHaveLength(5)
      expect(new Set(partida.opciones).size).toBe(5)
      expect(partida.opciones).toContain(partida.objetivo.nombreCorrecto)
      posiciones.add(partida.opciones.indexOf(partida.objetivo.nombreCorrecto))
    }
    expect(posiciones.size).toBeGreaterThan(1)
  })

  it('cuenta con los numeros del 1 al 10 en orden', () => {
    const ejercicios: EjercicioContar[] = [
      { id: 'a', imagen: 'a.png', pregunta: '¿Cuántas bananas hay?', cantidad: 2 },
      { id: 'b', imagen: 'b.png', pregunta: '¿Cuántos gatos hay?', cantidad: 3 },
    ]
    const partida = crearContar(ejercicios, null, mulberry32(1))
    expect(partida.opciones).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
    expect(mezclar([1, 2, 3], () => 0.999)).toEqual([1, 2, 3])
  })

  it('toma las cantidades reales del catalogo copiado', () => {
    const contar = JSON.parse(readFileSync(new URL('../../datos/contar.json', import.meta.url), 'utf8')) as {
      respuestas: string[]
    }[]
    expect(contar.map((item) => Number.parseInt(item.respuestas[0], 10))).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
    const sombras = JSON.parse(readFileSync(new URL('../../datos/sombras.json', import.meta.url), 'utf8')) as {
      juego_siluetas: { id: string; silueta_archivo: string }[]
    }
    expect(sombras.juego_siluetas.length).toBeGreaterThanOrEqual(5)
    expect(sombras.juego_siluetas.find((ficha) => ficha.id === 'delfin')?.silueta_archivo).toBe('Silueta_Delfin.png')
  })
})
