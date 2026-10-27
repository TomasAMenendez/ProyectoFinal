export type MotivoCamara = 'permiso' | 'ausente' | 'ocupada' | 'modelo' | 'navegador' | 'archivo'

const TEXTOS: Record<MotivoCamara, string> = {
  permiso: 'No pudimos usar la cámara. Podés permitirla y reintentar, o jugar con los botones.',
  ausente: 'Este dispositivo no tiene una cámara disponible. Podés jugar con los botones.',
  ocupada: 'La cámara está siendo usada por otra aplicación. Cerrala y reintentá, o jugá con los botones.',
  modelo: 'No pudimos preparar el reconocimiento en este dispositivo. Podés jugar con los botones.',
  navegador: 'Este navegador no puede reconocer la mano. Podés jugar con los botones.',
  archivo: 'En la tablet, Chrome no pide la cámara si el juego se abre como un archivo. Abrilo desde una página web, o seguí con los botones.',
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
