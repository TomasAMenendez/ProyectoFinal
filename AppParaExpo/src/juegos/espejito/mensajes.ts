import textos from '../../audio/textos-fijos.json'

export const CALIBRACION = textos.espejito.calibracion
export const SIN_CARA = textos.espejito.sinCara
export const PROBA = textos.espejito.probar
export const CIERRE = textos.espejito.cierre

export type MotivoCamara = 'permiso' | 'ausente' | 'ocupada' | 'modelo' | 'navegador' | 'archivo'

const TEXTOS: Record<MotivoCamara, string> = {
  permiso: 'No pudimos usar la cámara. Podés permitirla y reintentar, o salir.',
  ausente: 'Este dispositivo no tiene una cámara disponible. Podés salir y probar en otro.',
  ocupada: 'La cámara está siendo usada por otra aplicación. Cerrala y reintentá, o salí.',
  modelo: 'No pudimos preparar el reconocimiento en este dispositivo. Podés reintentar o salir.',
  navegador: 'Este navegador no puede usar la cámara. Podés salir y abrir el juego en otro.',
  archivo: 'En la tablet, Chrome no pide la cámara si el juego se abre como un archivo. Abrilo desde una página web.',
}

export function mensajeAyuda(hayCara: boolean): string {
  return hayCara ? PROBA : SIN_CARA
}

export function mensajeDe(motivo: MotivoCamara): string {
  return TEXTOS[motivo]
}

export function motivoDe(error: unknown): MotivoCamara {
  const nombre = error instanceof DOMException ? error.name : ''
  if (nombre === 'NotAllowedError' || nombre === 'SecurityError') return 'permiso'
  if (nombre === 'NotFoundError' || nombre === 'OverconstrainedError' || nombre === 'DevicesNotFoundError') return 'ausente'
  if (nombre === 'NotReadableError' || nombre === 'AbortError' || nombre === 'TrackStartError') return 'ocupada'
  return 'navegador'
}
