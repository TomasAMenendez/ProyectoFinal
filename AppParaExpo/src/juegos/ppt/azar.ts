import { FIGURAS, type FiguraId } from './figuras'
import { resultadoDe, type ResultadoPpt } from './reglas'

export interface CierreRonda {
  persona: FiguraId
  computadora: FiguraId
  resultado: ResultadoPpt
}

export function elegirComputadora(azar: () => number): FiguraId {
  const indice = Math.min(FIGURAS.length - 1, Math.floor(azar() * FIGURAS.length))
  return FIGURAS[Math.max(0, indice)].id
}

export function jugarSiLista(persona: FiguraId | null, azar: () => number): CierreRonda | null {
  if (!persona) return null
  const computadora = elegirComputadora(azar)
  return { persona, computadora, resultado: resultadoDe(persona, computadora) }
}
