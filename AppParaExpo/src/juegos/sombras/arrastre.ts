import type { Segmento } from './LineasUnion'

export const UMBRAL_ARRASTRE = 10

export interface CajaPalabra {
  id: string
  left: number
  top: number
  right: number
  bottom: number
  resuelto: boolean
}

export function esArrastre(inicio: { x: number; y: number }, actual: { x: number; y: number }): boolean {
  return Math.hypot(actual.x - inicio.x, actual.y - inicio.y) > UMBRAL_ARRASTRE
}

export function segmentoHastaDedo(
  origen: { x: number; y: number },
  dedo: { x: number; y: number },
  tablero: { left: number; top: number },
): Segmento {
  return {
    id: 'temporal',
    x1: origen.x,
    y1: origen.y,
    x2: dedo.x - tablero.left,
    y2: dedo.y - tablero.top,
  }
}

export function palabraBajoDedo(x: number, y: number, palabras: CajaPalabra[]): string | null {
  const encontrada = palabras.find(
    (caja) => !caja.resuelto && x >= caja.left && x <= caja.right && y >= caja.top && y <= caja.bottom,
  )
  return encontrada?.id ?? null
}
