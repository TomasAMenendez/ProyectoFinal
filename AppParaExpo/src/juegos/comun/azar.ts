export function mulberry32(semilla: number): () => number {
  let estado = semilla
  return () => {
    estado |= 0
    estado = (estado + 0x6d2b79f5) | 0
    let mezcla = Math.imul(estado ^ (estado >>> 15), 1 | estado)
    mezcla = (mezcla + Math.imul(mezcla ^ (mezcla >>> 7), 61 | mezcla)) ^ mezcla
    return ((mezcla ^ (mezcla >>> 14)) >>> 0) / 4294967296
  }
}
