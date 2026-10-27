import textos from '../../audio/textos-fijos.json'
import type { FiguraId } from './figuras'

export type ResultadoPpt = 'gano' | 'empato' | 'perdio'

const GANA: Record<FiguraId, FiguraId> = {
  piedra: 'tijera',
  papel: 'piedra',
  tijera: 'papel',
}

export function resultadoDe(persona: FiguraId, computadora: FiguraId): ResultadoPpt {
  if (persona === computadora) return 'empato'
  return GANA[persona] === computadora ? 'gano' : 'perdio'
}

export function textoResultado(resultado: ResultadoPpt): string {
  return textos.pptResultados[resultado]
}
