import { afterEach, describe, expect, it } from 'vitest'
import { limpiarVoces, reproducir, reproducirVeces } from './reproductor'

class AudioFalsa {
  static inicios: string[] = []
  static cortes: string[] = []
  static activas: AudioFalsa[] = []
  onended: (() => void) | null = null
  onpause: (() => void) | null = null
  onerror: (() => void) | null = null

  constructor(readonly url: string) {
    AudioFalsa.activas.push(this)
  }

  play(): Promise<void> {
    AudioFalsa.inicios.push(this.url)
    return Promise.resolve()
  }

  pause(): void {
    AudioFalsa.cortes.push(this.url)
    this.onpause?.()
  }

  terminar(): void {
    this.onended?.()
  }
}

describe('reproductor', () => {
  const original = globalThis.Audio

  afterEach(() => {
    limpiarVoces()
    AudioFalsa.inicios = []
    AudioFalsa.cortes = []
    AudioFalsa.activas = []
    globalThis.Audio = original
  })

  it('no suena si la apertura se cancela antes del turno', async () => {
    globalThis.Audio = AudioFalsa as unknown as typeof Audio
    const cancelar = reproducir('bienvenida')
    cancelar()
    await Promise.resolve()
    expect(AudioFalsa.inicios).toEqual([])
  })

  it('suena una sola vez si el efecto se cancela y se vuelve a pedir', async () => {
    globalThis.Audio = AudioFalsa as unknown as typeof Audio
    const primera = reproducir('bienvenida')
    primera()
    reproducir('bienvenida')
    await Promise.resolve()
    expect(AudioFalsa.inicios).toEqual(['bienvenida'])
  })

  it('el audio nuevo corta al que estaba sonando', async () => {
    globalThis.Audio = AudioFalsa as unknown as typeof Audio
    reproducir('vieja')
    await Promise.resolve()
    reproducir('nueva')
    await Promise.resolve()
    expect(AudioFalsa.cortes).toEqual(['vieja'])
    expect(AudioFalsa.inicios).toEqual(['vieja', 'nueva'])
  })

  it('si llegan juntas solo suena la ultima', async () => {
    globalThis.Audio = AudioFalsa as unknown as typeof Audio
    reproducir('vieja')
    reproducir('nueva')
    await Promise.resolve()
    expect(AudioFalsa.inicios).toEqual(['nueva'])
  })

  it('repite la frase cuando termina la primera', async () => {
    globalThis.Audio = AudioFalsa as unknown as typeof Audio
    reproducirVeces('vamos', 2)
    await Promise.resolve()
    expect(AudioFalsa.inicios).toEqual(['vamos'])
    AudioFalsa.activas.at(-1)?.terminar()
    await Promise.resolve()
    await Promise.resolve()
    expect(AudioFalsa.inicios).toEqual(['vamos', 'vamos'])
  })
})
