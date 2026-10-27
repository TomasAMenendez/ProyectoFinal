import { useEffect, useRef, useState } from 'react'
import type { PointerEvent as ReactPointerEvent } from 'react'
import { esArrastre, palabraBajoDedo, segmentoHastaDedo, type CajaPalabra } from './arrastre'
import type { Segmento } from './LineasUnion'

function salidaEnTablero(tablero: HTMLElement, elemento: Element): { x: number; y: number } {
  const base = tablero.getBoundingClientRect()
  const caja = elemento.getBoundingClientRect()
  return {
    x: caja.right - base.left - 14,
    y: caja.top + caja.height / 2 - base.top,
  }
}

function cajasDePalabras(tablero: HTMLElement | null): CajaPalabra[] {
  if (!tablero) return []
  return [...tablero.querySelectorAll<HTMLElement>('[data-nombre-id]')].map((elemento) => {
    const caja = elemento.getBoundingClientRect()
    return {
      id: elemento.dataset.nombreId ?? '',
      left: caja.left,
      top: caja.top,
      right: caja.right,
      bottom: caja.bottom,
      resuelto: elemento.dataset.resuelto === 'si',
    }
  })
}

export function useUnion(onUnir: (siluetaId: string, nombreId: string) => void) {
  const tableroRef = useRef<HTMLDivElement>(null)
  const [seleccion, setSeleccion] = useState<string | null>(null)
  const [temporal, setTemporal] = useState<Segmento | null>(null)
  const arrastrando = useRef(false)
  const origen = useRef<{ id: string; x: number; y: number } | null>(null)
  const inicio = useRef<{ x: number; y: number } | null>(null)
  const unirRef = useRef(onUnir)
  const limpiar = useRef<(() => void) | null>(null)
  unirRef.current = onUnir

  useEffect(() => () => limpiar.current?.(), [])

  function alBajar(id: string, event: ReactPointerEvent<HTMLButtonElement>) {
    if (!tableroRef.current || event.currentTarget.dataset.resuelto === 'si') return
    limpiar.current?.()
    const pointerId = event.pointerId
    try {
      event.currentTarget.setPointerCapture(pointerId)
    } catch {
      // En el celular el seguimiento continúa con los eventos de la ventana.
    }
    arrastrando.current = false
    inicio.current = { x: event.clientX, y: event.clientY }
    const punto = salidaEnTablero(tableroRef.current, event.currentTarget)
    origen.current = { id, ...punto }

    const mover = (puntero: PointerEvent) => {
      if (puntero.pointerId !== pointerId || !inicio.current || !origen.current || !tableroRef.current) return
      const dedo = { x: puntero.clientX, y: puntero.clientY }
      if (esArrastre(inicio.current, dedo)) arrastrando.current = true
      if (!arrastrando.current) return
      const base = tableroRef.current.getBoundingClientRect()
      setTemporal(segmentoHastaDedo(origen.current, dedo, base))
    }

    const soltar = (puntero: PointerEvent) => {
      if (puntero.pointerId !== pointerId) return
      cerrar()
      const origenId = origen.current?.id
      const fueArrastre = arrastrando.current
      setTemporal(null)
      inicio.current = null
      origen.current = null
      arrastrando.current = false
      if (!origenId) return
      if (!fueArrastre) {
        setSeleccion(origenId)
        return
      }
      const nombreId = palabraBajoDedo(puntero.clientX, puntero.clientY, cajasDePalabras(tableroRef.current))
      if (nombreId) unirRef.current(origenId, nombreId)
    }

    const cerrar = () => {
      window.removeEventListener('pointermove', mover)
      window.removeEventListener('pointerup', soltar)
      window.removeEventListener('pointercancel', soltar)
      limpiar.current = null
    }

    limpiar.current = cerrar
    window.addEventListener('pointermove', mover)
    window.addEventListener('pointerup', soltar)
    window.addEventListener('pointercancel', soltar)
  }

  function alElegirNombre(nombreId: string) {
    if (!seleccion) return
    const silueta = seleccion
    setSeleccion(null)
    unirRef.current(silueta, nombreId)
  }

  return { tableroRef, seleccion, temporal, setSeleccion, alBajar, alElegirNombre }
}
