import { describe, expect, it } from 'vitest'
import { mensajeAyuda, mensajeDe, motivoDe, PROBA, SIN_CARA } from './mensajes'

describe('mensajes de espejito', () => {
  it('pide acercarse o imitar segun haya cara', () => {
    expect(SIN_CARA).toBe('No te veo todavía. Acercate un poquito a la cámara.')
    expect(PROBA).toBe('¡Probá imitar la carita!')
    expect(mensajeAyuda(false)).toBe(SIN_CARA)
    expect(mensajeAyuda(true)).toBe(PROBA)
  })

  it('traduce los fallos de la camara sin ofrecer otro modo de juego', () => {
    expect(motivoDe(new DOMException('no', 'NotAllowedError'))).toBe('permiso')
    expect(motivoDe(new DOMException('no', 'NotFoundError'))).toBe('ausente')
    expect(motivoDe(new DOMException('no', 'NotReadableError'))).toBe('ocupada')
    expect(mensajeDe('modelo')).toMatch(/reconocimiento/i)
    expect(mensajeDe('navegador')).toMatch(/navegador/i)
    expect(mensajeDe('archivo')).toMatch(/archivo/i)
    expect(mensajeDe('archivo')).not.toMatch(/botones/i)
    expect(mensajeDe('permiso')).not.toMatch(/botones/i)
  })
})
