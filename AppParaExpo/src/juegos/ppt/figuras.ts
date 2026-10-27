export type FiguraId = 'piedra' | 'papel' | 'tijera'

export interface Figura {
  id: FiguraId
  nombre: string
  imagen: string
  clase: string
}

export const FIGURAS: Figura[] = [
  { id: 'piedra', nombre: 'Piedra', imagen: 'Imagen_Piedra.png', clase: 'Closed_Fist' },
  { id: 'papel', nombre: 'Papel', imagen: 'Imagen_Papel.png', clase: 'Open_Palm' },
  { id: 'tijera', nombre: 'Tijera', imagen: 'Imagen_Tijera.png', clase: 'Victory' },
]

export function figuraPorId(id: FiguraId): Figura {
  return FIGURAS.find((figura) => figura.id === id) ?? FIGURAS[0]
}

export function figuraPorClase(clase: string): FiguraId | null {
  const normal = clase.trim().toLowerCase()
  return FIGURAS.find((figura) => figura.clase.toLowerCase() === normal)?.id ?? null
}
