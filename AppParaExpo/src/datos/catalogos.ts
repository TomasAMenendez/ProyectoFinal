import adivinaJson from './adivina.json'
import contarJson from './contar.json'
import sombrasJson from './sombras.json'
import type { EjercicioContar, FichaAdivina, FichaSombra } from '../juegos/comun/tipos'

interface SombraCruda {
  id: string
  silueta_archivo: string
  respuestas: string[]
  pistas: string[]
}

interface AdivinaCruda {
  id: string
  imagen_archivo: string
  respuestas: string[]
  pistas: string[]
}

interface ContarCrudo {
  imagen_archivo: string
  pregunta: string
  respuestas: string[]
}

const imagenes = import.meta.glob('../recursos/imagenes/*.png', {
  eager: true,
  query: '?url',
  import: 'default',
}) as Record<string, string>

export function urlImagen(archivo: string): string {
  const entrada = Object.entries(imagenes).find(([ruta]) => ruta.endsWith(`/${archivo}`))
  return entrada?.[1] ?? ''
}

export function fichasSombra(): FichaSombra[] {
  return (sombrasJson as { juego_siluetas: SombraCruda[] }).juego_siluetas.map((ficha) => ({
    id: ficha.id,
    nombreVisible: ficha.respuestas[0],
    pistas: ficha.pistas,
    silueta: ficha.silueta_archivo,
  }))
}

export function fichasAdivina(): FichaAdivina[] {
  return (adivinaJson as AdivinaCruda[]).map((ficha) => ({
    id: ficha.id,
    nombreCorrecto: ficha.respuestas[0],
    pistas: ficha.pistas,
    imagen: ficha.imagen_archivo,
  }))
}

export function ejerciciosContar(): EjercicioContar[] {
  return (contarJson as ContarCrudo[]).map((ficha) => ({
    id: ficha.imagen_archivo,
    imagen: ficha.imagen_archivo,
    pregunta: ficha.pregunta,
    cantidad: Number.parseInt(ficha.respuestas[0], 10),
  }))
}
