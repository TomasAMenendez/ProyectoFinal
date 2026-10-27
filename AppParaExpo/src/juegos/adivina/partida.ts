import { pistasOrdenadas } from '../comun/pistas'
import { elegirUno, mezclar } from '../comun/sorteo'
import type { FichaAdivina, ResultadoPartida } from '../comun/tipos'
import { VIDAS_INICIALES, descontarVida } from '../comun/vidas'

export interface EstadoAdivina {
  objetivo: FichaAdivina
  opciones: string[]
  descartadas: string[]
  pistasVisibles: string[]
  vidas: number
  resultado: ResultadoPartida | null
}

export type EfectoAdivina =
  | { tipo: 'correcta'; termina: true }
  | { tipo: 'incorrecta'; pistaNueva: string | null; vidas: number; termina: boolean }
  | { tipo: 'repetida' }
  | { tipo: 'ignorada' }

export function crearAdivina(
  catalogo: readonly FichaAdivina[],
  evitarId: string | null,
  rng: () => number,
): EstadoAdivina {
  const objetivo = elegirUno(
    catalogo,
    evitarId ? (catalogo.find((ficha) => ficha.id === evitarId) ?? null) : null,
    (a, b) => a.id === b.id,
    rng,
  )
  const distractores: string[] = []
  const vistos = new Set([objetivo.nombreCorrecto])
  for (const ficha of mezclar(
    catalogo.filter((item) => item.id !== objetivo.id),
    rng,
  )) {
    if (vistos.has(ficha.nombreCorrecto)) continue
    vistos.add(ficha.nombreCorrecto)
    distractores.push(ficha.nombreCorrecto)
    if (distractores.length === 4) break
  }
  const pistas = pistasOrdenadas(objetivo.pistas)
  return {
    objetivo,
    opciones: mezclar([objetivo.nombreCorrecto, ...distractores], rng),
    descartadas: [],
    pistasVisibles: pistas.slice(0, 1),
    vidas: VIDAS_INICIALES,
    resultado: null,
  }
}

export function elegirOpcion(estado: EstadoAdivina, nombre: string): {
  estado: EstadoAdivina
  efecto: EfectoAdivina
} {
  if (estado.resultado || !estado.opciones.includes(nombre)) {
    return { estado, efecto: { tipo: 'ignorada' } }
  }
  if (estado.descartadas.includes(nombre)) {
    return { estado, efecto: { tipo: 'repetida' } }
  }
  if (nombre === estado.objetivo.nombreCorrecto) {
    return {
      estado: { ...estado, resultado: 'gano' },
      efecto: { tipo: 'correcta', termina: true },
    }
  }
  const ordenadas = pistasOrdenadas(estado.objetivo.pistas)
  const pistaNueva = ordenadas[estado.pistasVisibles.length] ?? null
  const vidas = descontarVida(estado.vidas)
  return {
    estado: {
      ...estado,
      descartadas: [...estado.descartadas, nombre],
      pistasVisibles: pistaNueva ? [...estado.pistasVisibles, pistaNueva] : estado.pistasVisibles,
      vidas,
      resultado: vidas === 0 ? 'perdio' : null,
    },
    efecto: { tipo: 'incorrecta', pistaNueva, vidas, termina: vidas === 0 },
  }
}
