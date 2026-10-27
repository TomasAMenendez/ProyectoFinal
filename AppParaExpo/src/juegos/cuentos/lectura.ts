export interface Fragmento {
  texto: string
  peso: number
}

export interface TrozoVisible {
  indice: number
  texto: string
  activo: boolean
}

export function planDeLectura(texto: string): Fragmento[] {
  const lineas = texto
    .split(/\n+/)
    .map((linea) => linea.trim())
    .filter(Boolean)
  return lineas.flatMap((linea) => oracionesDe(linea).map((oracion) => ({ texto: oracion, peso: oracion.length })))
}

export function indiceEn(plan: Fragmento[], progreso: number): number {
  if (!plan.length) return 0
  if (progreso >= 1) return plan.length - 1
  const total = plan.reduce((suma, fragmento) => suma + fragmento.peso, 0)
  if (total <= 0) return 0
  const meta = Math.min(Math.max(progreso, 0), 1) * total
  let acumulado = 0
  for (let indice = 0; indice < plan.length; indice += 1) {
    acumulado += plan[indice].peso
    if (meta < acumulado) return indice
  }
  return plan.length - 1
}

export function ventanaDe(plan: Fragmento[], indice: number, cantidad = 2): TrozoVisible[] {
  if (!plan.length) return []
  const tope = Math.max(1, cantidad)
  const inicio = Math.max(0, Math.min(indice, plan.length - tope))
  return plan.slice(inicio, inicio + tope).map((fragmento, desplazamiento) => ({
    indice: inicio + desplazamiento,
    texto: fragmento.texto,
    activo: inicio + desplazamiento === indice,
  }))
}

function oracionesDe(linea: string): string[] {
  const partes = linea
    .split(/(?<=[.!?…])\s+/)
    .map((parte) => parte.trim())
    .filter(Boolean)
  return partes.length ? partes : [linea]
}
