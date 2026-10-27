import type { JuegoId } from '../comun/tipos'

export function anotaEnPlanilla(juego: JuegoId): boolean {
  return juego === 'sombras' || juego === 'adivina' || juego === 'contar' || juego === 'ppt' || juego === 'espejito'
}
