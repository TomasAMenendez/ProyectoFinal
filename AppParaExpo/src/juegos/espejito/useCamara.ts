import { useEffect, useRef, useState } from 'react'
import { motivoDe, type MotivoCamara } from './mensajes'
import { crearDetector, type CaraVista } from './reconocedor'

interface Props {
  activa: boolean
  reintento: number
  onCara: (muestra: Record<string, number> | null, momento: number) => void
  onError: (motivo: MotivoCamara) => void
}

export function useCamara({ activa, reintento, onCara, onError }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const caraRef = useRef(onCara)
  const errorRef = useRef(onError)
  const [lista, setLista] = useState(false)
  caraRef.current = onCara
  errorRef.current = onError

  useEffect(() => {
    if (!activa) return
    let cancelado = false
    let stream: MediaStream | null = null
    let timer = 0
    let ocupado = false
    let detector: { ver: (video: HTMLVideoElement, momento: number) => CaraVista; cerrar: () => void } | null = null

    const cortar = () => {
      window.clearTimeout(timer)
      stream?.getTracks().forEach((pista) => pista.stop())
      stream = null
      detector?.cerrar()
      detector = null
      if (videoRef.current) videoRef.current.srcObject = null
    }

    const arrancar = async () => {
      if (!window.isSecureContext) {
        errorRef.current('archivo')
        return
      }
      if (!navigator.mediaDevices?.getUserMedia) {
        errorRef.current('navegador')
        return
      }
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: { facingMode: { ideal: 'user' }, width: { ideal: 640 }, height: { ideal: 480 } },
        })
      } catch (error) {
        errorRef.current(motivoDe(error))
        return
      }
      if (cancelado) {
        cortar()
        return
      }
      const video = videoRef.current
      if (!video) {
        cortar()
        errorRef.current('navegador')
        return
      }
      video.srcObject = stream
      video.muted = true
      video.playsInline = true
      try {
        await video.play()
        detector = await crearDetector()
      } catch {
        cortar()
        if (!cancelado) errorRef.current('modelo')
        return
      }
      if (cancelado) {
        cortar()
        return
      }
      setLista(true)
      const tick = () => {
        timer = window.setTimeout(tick, 100)
        const cuadro = videoRef.current
        if (ocupado || !detector || !cuadro || cuadro.readyState < 2) return
        ocupado = true
        try {
          const momento = performance.now()
          caraRef.current(detector.ver(cuadro, momento).muestra, momento)
        } catch {
          cortar()
          errorRef.current('modelo')
        } finally {
          ocupado = false
        }
      }
      tick()
    }

    void arrancar()
    return () => {
      cancelado = true
      setLista(false)
      cortar()
    }
  }, [activa, reintento])

  return { videoRef, lista }
}
