import { pistaParaFallo } from '../comun/pistas'
import { elegirElementos, firmaOrden, mezclar } from '../comun/sorteo'
import type { FichaSombra, ResultadoPartida } from '../comun/tipos'
import { VIDAS_INICIALES, descontarVida } from '../comun/vidas'

export interface EstadoSombras {
  fichas: FichaSombra[]
  nombres: { id: string; nombre: string }[]
  uniones: string[]
  vidas: number
  resultado: ResultadoPartida | null
  pistasUsadas: Record<string, number>
  aviso: { pista: string } | null
}

export type EfectoSombras =
  | { tipo: 'correcta'; termina: boolean }
  | { tipo: 'incorrecta'; pista: string; vidas: number; termina: boolean }
  | { tipo: 'ignorada' }

export function crearSombras(
  catalogo: readonly FichaSombra[],
  evitar: readonly string[] | null,
  rng: () => number,
): EstadoSombras {
  const fichas = elegirElementos(catalogo, 5, (ficha) => ficha.id, evitar, rng)
  let nombres = mezclar(
    fichas.map((ficha) => ({ id: ficha.id, nombre: ficha.nombreVisible })),
    rng,
  )
  const original = firmaOrden(fichas.map((ficha) => ficha.id))
  let intentos = 0
  while (firmaOrden(nombres.map((nombre) => nombre.id)) === original && intentos < 8) {
    nombres = mezclar(nombres, rng)
    intentos += 1
  }
  if (firmaOrden(nombres.map((nombre) => nombre.id)) === original && nombres.length > 1) {
    nombres = [nombres[1], nombres[0], ...nombres.slice(2)]
  }
  return {
    fichas,
    nombres,
    uniones: [],
    vidas: VIDAS_INICIALES,
    resultado: null,
    pistasUsadas: {},
    aviso: null,
  }
}

export function unirSombras(estado: EstadoSombras, siluetaId: string, nombreId: string): {
  estado: EstadoSombras
  efecto: EfectoSombras
} {
  if (estado.resultado || estado.uniones.includes(siluetaId) || estado.uniones.includes(nombreId)) {
    return { estado, efecto: { tipo: 'ignorada' } }
  }
  if (!estado.fichas.some((ficha) => ficha.id === siluetaId) || !estado.nombres.some((nombre) => nombre.id === nombreId)) {
    return { estado, efecto: { tipo: 'ignorada' } }
  }
  if (siluetaId === nombreId) {
    const uniones = [...estado.uniones, siluetaId]
    const termina = uniones.length === estado.fichas.length
    return {
      estado: { ...estado, uniones, aviso: null, resultado: termina ? 'gano' : null },
      efecto: { tipo: 'correcta', termina },
    }
  }
  const ficha = estado.fichas.find((item) => item.id === siluetaId)
  const usadas = estado.pistasUsadas[siluetaId] ?? 0
  const pista = ficha ? (pistaParaFallo(ficha.pistas, usadas) ?? '') : ''
  const vidas = descontarVida(estado.vidas)
  return {
    estado: {
      ...estado,
      vidas,
      pistasUsadas: { ...estado.pistasUsadas, [siluetaId]: usadas + 1 },
      aviso: { pista },
      resultado: vidas === 0 ? 'perdio' : null,
    },
    efecto: { tipo: 'incorrecta', pista, vidas, termina: vidas === 0 },
  }
}
