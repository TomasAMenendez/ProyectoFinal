import { describe, expect, it, vi } from 'vitest'
import { registrarResultado } from './registro'
import type { ResultadoVisita } from '../juegos/comun/tipos'

const datos: ResultadoVisita = {
  nombre: 'Luz',
  juego: 'sombras',
  resultado: 'gano',
  fechaHora: '2026-10-25T21:44:00-03:00',
}

describe('registro', () => {
  it('no envia nada cuando la entrega esta deshabilitada', async () => {
    const fetchImpl = vi.fn()
    const enviado = await registrarResultado(
      datos,
      { habilitado: false, url: 'https://planilla.example', apiKey: 'secreta' },
      fetchImpl as unknown as typeof fetch,
    )
    expect(enviado).toBe(false)
    expect(fetchImpl).not.toHaveBeenCalled()
  })

  it('envia Nombre, Juego y Resultado aunque no haya clave', async () => {
    const fetchImpl = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>(async () => new Response('ok'))
    const enviado = await registrarResultado(
      datos,
      { habilitado: true, url: 'https://planilla.example', apiKey: '' },
      fetchImpl as unknown as typeof fetch,
    )
    expect(enviado).toBe(true)
    const init = fetchImpl.mock.calls[0][1]
    expect(init?.method).toBe('POST')
    expect(init?.headers).toMatchObject({ 'Content-Type': 'text/plain;charset=utf-8' })
    expect(JSON.parse(String(init?.body))).toEqual({
      Nombre: 'Luz',
      Juego: 'Sombras',
      Resultado: 'Ganó',
    })
  })

  it('anota piedra papel o tijera y el empate', async () => {
    const fetchImpl = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>(async () => new Response('ok'))
    await registrarResultado(
      { ...datos, juego: 'ppt', resultado: 'empato' },
      { habilitado: true, url: 'https://planilla.example', apiKey: '' },
      fetchImpl as unknown as typeof fetch,
    )
    expect(JSON.parse(String(fetchImpl.mock.calls[0][1]?.body))).toEqual({
      Nombre: 'Luz',
      Juego: 'Piedra, papel o tijera',
      Resultado: 'Empató',
    })
  })

  it('anota espejito como gano una vez por sesion completa', async () => {
    const fetchImpl = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>(async () => new Response('ok'))
    await registrarResultado(
      { ...datos, juego: 'espejito', resultado: 'gano' },
      { habilitado: true, url: 'https://planilla.example', apiKey: '' },
      fetchImpl as unknown as typeof fetch,
    )
    expect(JSON.parse(String(fetchImpl.mock.calls[0][1]?.body))).toEqual({
      Nombre: 'Luz',
      Juego: 'Espejito',
      Resultado: 'Ganó',
    })
  })
})
