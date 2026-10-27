export type JuegoId = 'sombras' | 'adivina' | 'contar' | 'ppt' | 'espejito'
export type ResultadoPartida = 'gano' | 'empato' | 'perdio'
export type Pantalla = 'nombre' | 'menu' | JuegoId | 'cuentos'

export interface FichaSombra {
  id: string
  nombreVisible: string
  pistas: string[]
  silueta: string
}

export interface FichaAdivina {
  id: string
  nombreCorrecto: string
  pistas: string[]
  imagen: string
}

export interface EjercicioContar {
  id: string
  imagen: string
  pregunta: string
  cantidad: number
}

export interface ResultadoVisita {
  nombre: string
  juego: JuegoId
  resultado: ResultadoPartida
  fechaHora: string
}
