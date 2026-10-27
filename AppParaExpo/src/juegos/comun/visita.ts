import type { Pantalla } from './tipos'

export interface Visita {
  nombre: string | null
  pantalla: Pantalla
}

export function nombreValido(entrada: string): string | null {
  const limpio = entrada.trim().replace(/\s+/g, ' ')
  if (!limpio) return null
  return Array.from(limpio).slice(0, 40).join('')
}

export function visitaInicial(): Visita {
  return { nombre: null, pantalla: 'nombre' }
}

export function confirmarNombre(visita: Visita, entrada: string): Visita {
  const nombre = nombreValido(entrada)
  if (!nombre) return visita
  return { nombre, pantalla: 'menu' }
}

export function abrirJuego(visita: Visita, pantalla: Exclude<Pantalla, 'nombre' | 'menu'>): Visita {
  if (!visita.nombre) return visita
  return { ...visita, pantalla }
}

export function volverAlMenu(visita: Visita): Visita {
  if (!visita.nombre) return visitaInicial()
  return { nombre: visita.nombre, pantalla: 'menu' }
}
