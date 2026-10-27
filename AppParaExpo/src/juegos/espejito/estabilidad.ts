export interface Avance {
  progreso: number
  ultimo: number | null
}

const TOPE_MS = 250
const SUBIDA_MS = 1000
const BAJADA_MS = 2000

export function avanceInicial(): Avance {
  return { progreso: 0, ultimo: null }
}

export function actualizarAvance(avance: Avance, activo: boolean, ahora: number): Avance {
  const transcurrido = avance.ultimo == null ? 0 : Math.min(Math.max(0, ahora - avance.ultimo), TOPE_MS)
  const delta = activo ? transcurrido / SUBIDA_MS : -transcurrido / BAJADA_MS
  const progreso = Math.min(1, Math.max(0, avance.progreso + delta))
  return { progreso: progreso >= 1 - 1e-9 ? 1 : progreso, ultimo: ahora }
}

export function confirmado(avance: Avance): boolean {
  return avance.progreso >= 1
}
