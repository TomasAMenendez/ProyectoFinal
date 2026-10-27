const ORDEN = [1, 3, 2]

export function pistasOrdenadas(pistas: readonly string[]): string[] {
  const usadas = new Set<number>()
  const salida: string[] = []
  for (const numero of ORDEN) {
    const indice = numero - 1
    if (indice < 0 || indice >= pistas.length || !pistas[indice] || usadas.has(indice)) continue
    usadas.add(indice)
    salida.push(pistas[indice])
  }
  return salida
}

export function pistaParaFallo(pistas: readonly string[], fallosPrevios: number): string | null {
  const ordenadas = pistasOrdenadas(pistas)
  if (ordenadas.length === 0) return null
  if (fallosPrevios < ordenadas.length) return ordenadas[fallosPrevios]
  return ordenadas[ordenadas.length - 1]
}
