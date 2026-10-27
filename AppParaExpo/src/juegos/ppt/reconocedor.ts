import { prepararVision } from 'virtual:vision-embebida'
import { figuraPorClase, type FiguraId } from './figuras'

export interface ManoVista {
  hayMano: boolean
  figura: FiguraId | null
  confianza: number
}

interface Categoria {
  categoryName?: string
  score?: number
}

interface ResultadoCrudo {
  landmarks?: unknown[][]
  gestures?: Categoria[][]
}

export interface ReconocedorActivo {
  ver(video: HTMLVideoElement, momento: number): ManoVista
  cerrar(): void
}

export function manoDesdeResultado(resultado: ResultadoCrudo): ManoVista {
  const hayMano = (resultado.landmarks?.length ?? 0) > 0
  const mejor = resultado.gestures?.[0]?.[0]
  if (!hayMano || !mejor?.categoryName) return { hayMano, figura: null, confianza: 0 }
  return {
    hayMano: true,
    figura: figuraPorClase(mejor.categoryName),
    confianza: mejor.score ?? 0,
  }
}

export async function crearReconocedor(): Promise<ReconocedorActivo> {
  const vision = await import('@mediapipe/tasks-vision')
  const preparado = await prepararVision('mano')
  const comun = { runningMode: 'VIDEO' as const, numHands: 1 }
  let reconocedor
  try {
    reconocedor = await vision.GestureRecognizer.createFromOptions(preparado.fileset, {
      ...comun,
      baseOptions: { ...preparado.modelo, delegate: 'GPU' },
    })
  } catch {
    reconocedor = await vision.GestureRecognizer.createFromOptions(preparado.fileset, {
      ...comun,
      baseOptions: { ...preparado.modelo, delegate: 'CPU' },
    })
  }
  return {
    ver(video, momento) {
      return manoDesdeResultado(reconocedor.recognizeForVideo(video, momento))
    },
    cerrar() {
      try {
        reconocedor.close()
      } catch {
        // El reconocedor ya estaba cerrado.
      }
    },
  }
}
