import { describe, expect, it } from 'vitest'
import { mensajeDe, motivoDe } from './mensajes'

describe('mensajes de la camara', () => {
  it('explica permiso, ausencia, ocupacion, modelo y navegador', () => {
    expect(mensajeDe('permiso')).toMatch(/cámara/i)
    expect(mensajeDe('ausente')).toMatch(/cámara/i)
    expect(mensajeDe('ocupada')).toMatch(/otra aplicación/i)
    expect(mensajeDe('modelo')).toMatch(/reconocimiento/i)
    expect(mensajeDe('navegador')).toMatch(/navegador/i)
    expect(mensajeDe('archivo')).toMatch(/archivo/i)
    for (const motivo of ['permiso', 'ausente', 'ocupada', 'modelo', 'navegador', 'archivo'] as const) {
      expect(mensajeDe(motivo).length).toBeGreaterThan(20)
    }
  })

  it('traduce los fallos del navegador', () => {
    expect(motivoDe(new DOMException('no', 'NotAllowedError'))).toBe('permiso')
    expect(motivoDe(new DOMException('no', 'NotFoundError'))).toBe('ausente')
    expect(motivoDe(new DOMException('no', 'NotReadableError'))).toBe('ocupada')
    expect(motivoDe(new DOMException('no', 'AbortError'))).toBe('ocupada')
    expect(motivoDe(new Error('wasm'))).toBe('navegador')
  })
})
