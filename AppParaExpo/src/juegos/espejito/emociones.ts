import textos from '../../audio/textos-fijos.json'

export type EmocionId = 'feliz' | 'triste' | 'enojado' | 'sorprendido'

export interface Emocion {
  id: EmocionId
  nombre: string
  imagen: string
  instruccion: string
  felicitacion: string
  umbral: number
  pesos: Record<string, number>
}

const frases = textos.espejito

export const EMOCIONES: Emocion[] = [
  {
    id: 'feliz',
    nombre: 'Feliz',
    imagen: 'Emocion_Feliz.png',
    instruccion: frases.instrucciones.feliz,
    felicitacion: frases.felicitaciones.feliz,
    umbral: 0.18,
    pesos: { mouthSmileLeft: 0.5, mouthSmileRight: 0.5 },
  },
  {
    id: 'triste',
    nombre: 'Triste',
    imagen: 'Emocion_Triste.png',
    instruccion: frases.instrucciones.triste,
    felicitacion: frases.felicitaciones.triste,
    umbral: 0.06,
    pesos: { mouthFrownLeft: 0.5, mouthFrownRight: 0.5, mouthShrugLower: 1 },
  },
  {
    id: 'enojado',
    nombre: 'Enojado',
    imagen: 'Emocion_Enojado.png',
    instruccion: frases.instrucciones.enojado,
    felicitacion: frases.felicitaciones.enojado,
    umbral: 0.16,
    pesos: { browDownLeft: 0.35, browDownRight: 0.35, noseSneerLeft: 0.15, noseSneerRight: 0.15 },
  },
  {
    id: 'sorprendido',
    nombre: 'Sorprendido',
    imagen: 'Emocion_Sorprendido.png',
    instruccion: frases.instrucciones.sorprendido,
    felicitacion: frases.felicitaciones.sorprendido,
    umbral: 0.1,
    pesos: { jawOpen: 0.5, eyeWideLeft: 0.25, eyeWideRight: 0.25 },
  },
]

export function emocionPorId(id: EmocionId): Emocion {
  return EMOCIONES.find((emocion) => emocion.id === id) ?? EMOCIONES[0]
}
