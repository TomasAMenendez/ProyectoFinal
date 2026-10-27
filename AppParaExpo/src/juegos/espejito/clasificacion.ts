import { EMOCIONES, type EmocionId } from './emociones'

export type Muestra = Record<string, number>
export type Puntuaciones = Record<EmocionId, number>

function incremento(actual: Muestra, referencia: Muestra, nombre: string): number {
  return Math.max(0, (actual[nombre] ?? 0) - (referencia[nombre] ?? 0))
}

function nota(emocion: (typeof EMOCIONES)[number], actual: Muestra, referencia: Muestra): number {
  if (emocion.id === 'triste') {
    const boca =
      (incremento(actual, referencia, 'mouthFrownLeft') +
        incremento(actual, referencia, 'mouthFrownRight')) /
      2
    const labio = incremento(actual, referencia, 'mouthShrugLower')
    return Math.max(boca, labio)
  }
  let suma = 0
  let pesos = 0
  for (const [nombre, peso] of Object.entries(emocion.pesos)) {
    suma += incremento(actual, referencia, nombre) * peso
    pesos += peso
  }
  return pesos > 0 ? suma / pesos : 0
}

export function puntuaciones(actual: Muestra, referencia: Muestra = {}): Puntuaciones {
  const salida = {} as Puntuaciones
  for (const emocion of EMOCIONES) salida[emocion.id] = nota(emocion, actual, referencia)
  return salida
}

export function cumple(id: EmocionId, puntos: Puntuaciones): boolean {
  const emocion = EMOCIONES.find((item) => item.id === id)
  if (!emocion) return false
  const rivales = EMOCIONES.filter((item) => !(id === 'triste' && item.id === 'enojado'))
  const mayor = Math.max(...rivales.map((item) => puntos[item.id]))
  return puntos[id] >= emocion.umbral && puntos[id] >= mayor * 0.85
}
