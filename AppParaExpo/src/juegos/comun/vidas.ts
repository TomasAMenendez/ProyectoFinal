export const VIDAS_INICIALES = 3

export type LadoCorazon = 'izquierda' | 'medio' | 'derecha'

export function descontarVida(vidas: number): number {
  return Math.max(0, vidas - 1)
}

export function corazonQueSeRompe(vidasAntes: number): LadoCorazon | null {
  if (vidasAntes === 3) return 'derecha'
  if (vidasAntes === 2) return 'medio'
  if (vidasAntes === 1) return 'izquierda'
  return null
}
