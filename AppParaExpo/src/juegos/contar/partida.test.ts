import { describe, expect, it } from 'vitest'
import { mulberry32 } from '../comun/azar'
import type { EjercicioContar } from '../comun/tipos'
import { crearContar, elegirNumero } from './partida'

const ejercicios: EjercicioContar[] = [
  { id: 'bananas', imagen: 'bananas.png', pregunta: '¿Cuántas bananas hay?', cantidad: 2 },
  { id: 'gatos', imagen: 'gatos.png', pregunta: '¿Cuántos gatos hay?', cantidad: 3 },
]

describe('contar', () => {
  it('usa la cantidad del ejercicio y gana con ese numero', () => {
    const inicio = crearContar(ejercicios, null, () => 0)
    expect(inicio.ejercicio.cantidad).toBe(2)
    expect(inicio.opciones).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
    expect(elegirNumero(inicio, 2).estado.resultado).toBe('gano')
  })

  it('apaga solo el numero incorrecto', () => {
    const inicio = crearContar(ejercicios, null, () => 0)
    const fallo = elegirNumero(inicio, 5)
    expect(fallo.estado.descartadas).toEqual([5])
    expect(fallo.estado.vidas).toBe(2)
    const repetido = elegirNumero(fallo.estado, 5)
    expect(repetido.efecto.tipo).toBe('repetida')
    expect(repetido.estado.vidas).toBe(2)
    expect(elegirNumero(fallo.estado, 4).estado.opciones).toHaveLength(10)
  })

  it('pierde al tercer numero incorrecto y cambia de ejercicio', () => {
    const inicio = crearContar(ejercicios, null, () => 0)
    let estado = inicio
    for (const numero of [1, 4, 5]) estado = elegirNumero(estado, numero).estado
    expect(estado.resultado).toBe('perdio')
    const siguiente = crearContar(ejercicios, inicio.ejercicio.id, mulberry32(9))
    expect(siguiente.ejercicio.id).not.toBe(inicio.ejercicio.id)
    expect(siguiente.vidas).toBe(3)
    expect(siguiente.descartadas).toEqual([])
  })
})
