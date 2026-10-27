import { prepararVision } from 'virtual:vision-embebida'

export interface CaraVista {
  muestra: Record<string, number> | null
}

interface Categoria {
  categoryName?: string
  score?: number
}

interface ResultadoCara {
  faceLandmarks?: unknown[][]
  faceBlendshapes?: { categories?: Categoria[] }[]
}

export interface DetectorActivo {
  ver(video: HTMLVideoElement, momento: number): CaraVista
  cerrar(): void
}

export function caraDesdeResultado(resultado: ResultadoCara): CaraVista {
  const categorias = resultado.faceBlendshapes?.[0]?.categories
  const hayCara = (resultado.faceLandmarks?.length ?? 0) > 0 && (categorias?.length ?? 0) > 0
  if (!hayCara || !categorias) return { muestra: null }
  const muestra: Record<string, number> = {}
  for (const categoria of categorias) {
    if (!categoria.categoryName) continue
    muestra[categoria.categoryName] = categoria.score ?? 0
  }
  return { muestra }
}

export async function crearDetector(): Promise<DetectorActivo> {
  const vision = await import('@mediapipe/tasks-vision')
  const preparado = await prepararVision('cara')
  const comun = { runningMode: 'VIDEO' as const, numFaces: 1, outputFaceBlendshapes: true }
  let detector
  try {
    detector = await vision.FaceLandmarker.createFromOptions(preparado.fileset, {
      ...comun,
      baseOptions: { ...preparado.modelo, delegate: 'GPU' },
    })
  } catch {
    detector = await vision.FaceLandmarker.createFromOptions(preparado.fileset, {
      ...comun,
      baseOptions: { ...preparado.modelo, delegate: 'CPU' },
    })
  }
  return {
    ver(video, momento) {
      return caraDesdeResultado(detector.detectForVideo(video, momento))
    },
    cerrar() {
      try {
        detector.close()
      } catch {
        // El detector ya estaba cerrado.
      }
    },
  }
}
