import type { FiguraId } from './figuras'

export const UMBRAL_CONFIANZA = 0.7
export const CONFIRMACION_MS = 800
export const PASO_CUENTA_MS = 1000

export interface ObservacionGesto {
  figura: FiguraId | null
  confianza: number
  momento: number
}

export interface Tramo {
  figura: FiguraId | null
  desde: number | null
}

export interface Cuenta {
  figura: FiguraId
  numero: 3 | 2 | 1
  desde: number
}

function valida(obs: ObservacionGesto, figura?: FiguraId): boolean {
  return obs.figura !== null && obs.confianza >= UMBRAL_CONFIANZA && (figura === undefined || obs.figura === figura)
}

export function avanzarTramo(tramo: Tramo, obs: ObservacionGesto): { tramo: Tramo; confirmado: boolean } {
  if (!valida(obs)) return { tramo: { figura: null, desde: null }, confirmado: false }
  if (tramo.figura !== obs.figura || tramo.desde === null) {
    return { tramo: { figura: obs.figura, desde: obs.momento }, confirmado: false }
  }
  return { tramo, confirmado: obs.momento - tramo.desde >= CONFIRMACION_MS }
}

export function iniciarCuenta(figura: FiguraId, momento: number): Cuenta {
  return { figura, numero: 3, desde: momento }
}

export function vigilarCuenta(
  cuenta: Cuenta,
  obs: ObservacionGesto,
): { estado: 'sigue'; cuenta: Cuenta } | { estado: 'cancelada' } | { estado: 'lista' } {
  if (!valida(obs, cuenta.figura)) return { estado: 'cancelada' }
  if (obs.momento - cuenta.desde < PASO_CUENTA_MS) return { estado: 'sigue', cuenta }
  if (cuenta.numero === 1) return { estado: 'lista' }
  const numero = (cuenta.numero - 1) as 2 | 1
  return { estado: 'sigue', cuenta: { figura: cuenta.figura, numero, desde: obs.momento } }
}
