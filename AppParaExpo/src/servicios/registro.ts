import type { JuegoId, ResultadoPartida, ResultadoVisita } from '../juegos/comun/tipos'
import { configDeCompilacion, type ConfigRegistro } from '@registro-config'

export type { ConfigRegistro }
export { configDeCompilacion }

const JUEGO: Record<JuegoId, string> = {
  sombras: 'Sombras',
  adivina: 'Adivina el objeto',
  contar: 'Contar',
  ppt: 'Piedra, papel o tijera',
  espejito: 'Espejito',
}

const RESULTADO: Record<ResultadoPartida, string> = {
  gano: 'Ganó',
  empato: 'Empató',
  perdio: 'Perdió',
}

export function fechaLocalIso(fecha = new Date()): string {
  const pad = (valor: number) => String(valor).padStart(2, '0')
  const zona = -fecha.getTimezoneOffset()
  const signo = zona >= 0 ? '+' : '-'
  const absoluto = Math.abs(zona)
  return `${fecha.getFullYear()}-${pad(fecha.getMonth() + 1)}-${pad(fecha.getDate())}T${pad(fecha.getHours())}:${pad(fecha.getMinutes())}:${pad(fecha.getSeconds())}${signo}${pad(Math.floor(absoluto / 60))}:${pad(absoluto % 60)}`
}

export async function registrarResultado(
  datos: ResultadoVisita,
  config: ConfigRegistro,
  fetchImpl: typeof fetch = fetch,
): Promise<boolean> {
  if (!config.habilitado || !config.url) return false
  const cuerpo = JSON.stringify({
    Nombre: datos.nombre,
    Juego: JUEGO[datos.juego],
    Resultado: RESULTADO[datos.resultado],
  })
  try {
    await fetchImpl(config.url, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain;charset=utf-8' },
      body: cuerpo,
    })
  } catch {
    return false
  }
  return true
}
