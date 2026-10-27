import { describe, expect, it } from 'vitest'
import { mulberry32 } from '../comun/azar'
import type { FichaAdivina } from '../comun/tipos'
import { crearAdivina, elegirOpcion } from './partida'

const catalogo: FichaAdivina[] = Array.from({ length: 6 }, (_, indice) => ({
  id: `obj-${indice}`,
  nombreCorrecto: `objeto-${indice}`,
  pistas: ['primera', 'segunda', 'tercera'],
  imagen: `${indice}.png`,
}))

describe('adivina', () => {
  it('empieza con la primera pista y gana al acertar', () => {
    const inicio = crearAdivina(catalogo, null, mulberry32(8))
    expect(inicio.pistasVisibles).toEqual(['primera'])
    const gano = elegirOpcion(inicio, inicio.objetivo.nombreCorrecto)
    expect(gano.estado.resultado).toBe('gano')
  })

  it('acumula pistas, apaga la opcion y no vuelve a descontar', () => {
    const inicio = crearAdivina(catalogo, null, mulberry32(8))
    const incorrecta = inicio.opciones.find((opcion) => opcion !== inicio.objetivo.nombreCorrecto) ?? ''
    const primerFallo = elegirOpcion(inicio, incorrecta)
    expect(primerFallo.estado.descartadas).toContain(incorrecta)
    expect(primerFallo.estado.pistasVisibles).toEqual(['primera', 'tercera'])
    expect(primerFallo.estado.vidas).toBe(2)
    const repetida = elegirOpcion(primerFallo.estado, incorrecta)
    expect(repetida.efecto.tipo).toBe('repetida')
    expect(repetida.estado.vidas).toBe(2)
    const otra = inicio.opciones.find((opcion) => opcion !== incorrecta && opcion !== inicio.objetivo.nombreCorrecto) ?? ''
    const segundo = elegirOpcion(primerFallo.estado, otra)
    expect(segundo.estado.pistasVisibles).toEqual(['primera', 'tercera', 'segunda'])
  })

  it('pierde al tercer error', () => {
    const inicio = crearAdivina(catalogo, null, mulberry32(8))
    const malas = inicio.opciones.filter((opcion) => opcion !== inicio.objetivo.nombreCorrecto)
    let estado = inicio
    for (const opcion of malas.slice(0, 3)) {
      estado = elegirOpcion(estado, opcion).estado
    }
    expect(estado.resultado).toBe('perdio')
  })
})
