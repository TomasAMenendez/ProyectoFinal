export function mezclar<T>(items: readonly T[], rng: () => number): T[] {
  const copia = [...items]
  for (let i = copia.length - 1; i > 0; i -= 1) {
    const j = Math.floor(rng() * (i + 1))
    const actual = copia[i]
    copia[i] = copia[j]
    copia[j] = actual
  }
  return copia
}

export function conjuntosIguales(a: readonly string[], b: readonly string[]): boolean {
  if (a.length !== b.length) return false
  const izquierda = [...a].sort()
  const derecha = [...b].sort()
  return izquierda.every((valor, indice) => valor === derecha[indice])
}

export function elegirElementos<T>(
  items: readonly T[],
  cantidad: number,
  id: (item: T) => string,
  evitar: readonly string[] | null,
  rng: () => number,
): T[] {
  if (items.length < cantidad) {
    throw new Error('No hay elementos suficientes para armar la partida')
  }
  const tomar = () => {
    const elegidos: T[] = []
    const vistos = new Set<string>()
    for (const item of mezclar(items, rng)) {
      const clave = id(item)
      if (vistos.has(clave)) continue
      vistos.add(clave)
      elegidos.push(item)
      if (elegidos.length === cantidad) break
    }
    return elegidos
  }
  let elegidos = tomar()
  let intentos = 0
  while (evitar && conjuntosIguales(elegidos.map(id), evitar) && intentos < 12) {
    elegidos = tomar()
    intentos += 1
  }
  return elegidos
}

export function elegirUno<T>(
  items: readonly T[],
  evitar: T | null,
  igual: (a: T, b: T) => boolean,
  rng: () => number,
): T {
  if (items.length === 0) throw new Error('No hay elementos para sortear')
  let elegido = items[Math.floor(rng() * items.length)]
  let intentos = 0
  while (evitar && igual(elegido, evitar) && items.length > 1 && intentos < 12) {
    elegido = items[Math.floor(rng() * items.length)]
    intentos += 1
  }
  return elegido
}

export function firmaOrden(ids: readonly string[]): string {
  return ids.join('|')
}
