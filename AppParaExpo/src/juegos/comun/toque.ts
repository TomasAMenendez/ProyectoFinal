export function aceptarToque(ultimo: number, ahora: number, espera = 350): boolean {
  return ahora - ultimo >= espera
}
