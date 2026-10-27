import { elegirUno } from '../comun/sorteo'
import type { EjercicioContar, ResultadoPartida } from '../comun/tipos'
import { VIDAS_INICIALES, descontarVida } from '../comun/vidas'

export const NUMEROS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] as const

export interface EstadoContar {
  ejercicio: EjercicioContar
  opciones: number[]
  descartadas: number[]
  vidas: number
  resultado: ResultadoPartida | null
}

export type EfectoContar =
  | { tipo: 'correcta'; termina: true }
  | { tipo: 'incorrecta'; vidas: number; termina: boolean }
  | { tipo: 'repetida' }
  | { tipo: 'ignorada' }

export function crearContar(
  catalogo: readonly EjercicioContar[],
  evitarId: string | null,
  rng: () => number,
): EstadoContar {
  const evitar = evitarId ? (catalogo.find((item) => item.id === evitarId) ?? null) : null
  const ejercicio = elegirUno(catalogo, evitar, (a, b) => a.id === b.id, rng)
  return {
    ejercicio,
    opciones: [...NUMEROS],
    descartadas: [],
    vidas: VIDAS_INICIALES,
    resultado: null,
  }
}

export function elegirNumero(estado: EstadoContar, numero: number): {
  estado: EstadoContar
  efecto: EfectoContar
} {
  if (estado.resultado || !estado.opciones.includes(numero)) {
    return { estado, efecto: { tipo: 'ignorada' } }
  }
  if (estado.descartadas.includes(numero)) {
    return { estado, efecto: { tipo: 'repetida' } }
  }
  if (numero === estado.ejercicio.cantidad) {
    return {
      estado: { ...estado, resultado: 'gano' },
      efecto: { tipo: 'correcta', termina: true },
    }
  }
  const vidas = descontarVida(estado.vidas)
  return {
    estado: {
      ...estado,
      descartadas: [...estado.descartadas, numero],
      vidas,
      resultado: vidas === 0 ? 'perdio' : null,
    },
    efecto: { tipo: 'incorrecta', vidas, termina: vidas === 0 },
  }
}
