import { EMOCIONES, type EmocionId } from './emociones'

export interface Sesion {
  orden: EmocionId[]
  indice: number
  cerrada: boolean
}

export function sesionNueva(azar: () => number): Sesion {
  const indice = Math.min(EMOCIONES.length - 1, Math.max(0, Math.floor(azar() * EMOCIONES.length)))
  return { orden: [EMOCIONES[indice].id], indice: 0, cerrada: false }
}

export function siguiente(sesion: Sesion): { sesion: Sesion; anotar: boolean } {
  if (sesion.cerrada) return { sesion, anotar: false }
  const indice = sesion.indice + 1
  const cerrada = indice >= sesion.orden.length
  return { sesion: { ...sesion, indice, cerrada }, anotar: cerrada }
}
