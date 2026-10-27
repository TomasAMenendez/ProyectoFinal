let actual: HTMLAudioElement | null = null
let turno = 0

export function reproducir(url: string | undefined): () => void {
  if (!url) return () => {}
  return reproducirVeces(url, 1)
}

export function reproducirVeces(url: string | undefined, veces: number): () => void {
  if (!url || veces < 1 || typeof Audio === 'undefined') return () => {}
  const ticket = ++turno
  let vigente = true
  queueMicrotask(() => {
    if (!vigente || ticket !== turno) return
    cortar()
    void sonarLista(Array.from({ length: veces }, () => url), ticket)
  })
  return () => {
    vigente = false
  }
}

export function limpiarVoces(): void {
  turno += 1
  cortar()
}

function cortar(): void {
  if (!actual) return
  const audio = actual
  actual = null
  audio.pause()
}

async function sonarLista(urls: string[], ticket: number): Promise<void> {
  for (const url of urls) {
    if (ticket !== turno) return
    const audio = new Audio(url)
    actual = audio
    let corto = false
    const fin = new Promise<void>((resolver) => {
      audio.onended = () => resolver()
      audio.onpause = () => {
        corto = true
        resolver()
      }
      audio.onerror = () => resolver()
    })
    try {
      await audio.play()
      await fin
    } catch {
      if (actual === audio) actual = null
      return
    }
    if (corto || ticket !== turno) return
    if (actual === audio) actual = null
  }
}
