import { describe, expect, it } from 'vitest'
import { mulberry32 } from '../comun/azar'
import type { FichaSombra } from '../comun/tipos'
import { crearSombras, unirSombras } from './partida'

function ficha(id: string, pistas = ['primera', 'segunda']): FichaSombra {
  return { id, nombreVisible: id, pistas, silueta: `${id}.png` }
}

const catalogo = ['perro', 'gato', 'pez', 'ave', 'oso', 'vaca'].map((id) => ficha(id))

describe('sombras', () => {
  it('acepta la pareja correcta y bloquea lo resuelto', () => {
    const inicio = crearSombras(catalogo, null, mulberry32(3))
    const id = inicio.fichas[0].id
    const correcta = unirSombras(inicio, id, id)
    expect(correcta.efecto.tipo).toBe('correcta')
    expect(correcta.estado.uniones).toContain(id)
    const repetida = unirSombras(correcta.estado, id, id)
    expect(repetida.efecto.tipo).toBe('ignorada')
    expect(repetida.estado.vidas).toBe(3)
  })

  it('muestra la pista de esa silueta y descuenta una vida', () => {
    const inicio = crearSombras(catalogo, null, mulberry32(3))
    const [primera, segunda] = inicio.fichas
    const fallo = unirSombras(inicio, primera.id, segunda.id)
    expect(fallo.efecto.tipo).toBe('incorrecta')
    if (fallo.efecto.tipo === 'incorrecta') expect(fallo.efecto.pista).toBe('primera')
    expect(fallo.estado.aviso?.pista).toBe('primera')
    expect(fallo.estado.uniones).toHaveLength(0)
    expect(fallo.estado.vidas).toBe(2)
    const otro = unirSombras(fallo.estado, primera.id, inicio.fichas[2].id)
    expect(otro.estado.aviso?.pista).toBe('segunda')
    expect(otro.estado.vidas).toBe(1)
  })

  it('gana con las cinco uniones y pierde a la tercera falla', () => {
    const inicio = crearSombras(catalogo, null, mulberry32(3))
    let estado = inicio
    for (const fichaActual of inicio.fichas) {
      const paso = unirSombras(estado, fichaActual.id, fichaActual.id)
      estado = paso.estado
    }
    expect(estado.resultado).toBe('gano')

    let perdida = inicio
    const ajenos = inicio.fichas.map((item) => item.id)
    for (let indice = 0; indice < 3; indice += 1) {
      const paso = unirSombras(perdida, ajenos[indice], ajenos[(indice + 1) % 5])
      perdida = paso.estado
    }
    expect(perdida.resultado).toBe('perdio')
    expect(perdida.vidas).toBe(0)
  })
})
