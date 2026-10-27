import textos from './textos-fijos.json'
import indice from '../recursos/voces/indice.json'
import type { JuegoId } from '../juegos/comun/tipos'

const voces = import.meta.glob('../recursos/voces/*.mp3', {
  eager: true,
  query: '?url',
  import: 'default',
}) as Record<string, string>

const cursores = new Map<string, number>()

export const APERTURA = textos.apertura
export const VAMOS_A_JUGAR = textos.vamosAJugar
export const AVISO_PISTA = textos.avisoPista

export function bienvenida(juego: JuegoId): string {
  return textos.bienvenidas[juego]
}

export function numeroHablado(numero: number): string {
  return textos.numeros[String(numero) as keyof typeof textos.numeros] ?? String(numero)
}

export function variante(grupo: string, frases: readonly string[]): string {
  const actual = cursores.get(grupo) ?? 0
  cursores.set(grupo, (actual + 1) % frases.length)
  return frases[actual]
}

export function fraseAcierto(): string {
  return variante('acierto', textos.aciertos)
}

export function fraseError(): string {
  return variante('error', textos.errores)
}

export function fraseDescartada(): string {
  return variante('descartada', textos.descartadas)
}

export function fraseRejuego(): string {
  return variante('rejuego', textos.rejuegos)
}

export function fraseMenu(): string {
  return variante('menu', textos.menus)
}

export function fraseVictoria(): string {
  return variante('victoria', textos.victorias)
}

export function fraseDerrota(): string {
  return variante('derrota', textos.derrotas)
}

export function urlVoz(texto: string): string | undefined {
  const archivo = (indice as Record<string, string>)[texto]
  if (!archivo) return undefined
  const entrada = Object.entries(voces).find(([ruta]) => ruta.endsWith(`/${archivo}`))
  return entrada?.[1]
}
