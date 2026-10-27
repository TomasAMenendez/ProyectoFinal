export type Muestra = Record<string, number>

export interface Calibracion {
  apertura: number
  primera: number | null
  muestras: Muestra[]
}

export interface PasoCalibracion {
  estado: Calibracion
  lista: boolean
  referencia?: Muestra
}

const MUESTRAS = 5
const ESPERA_MS = 1500
const TOPE_MS = 8000

export function calibracionNueva(ahora: number): Calibracion {
  return { apertura: ahora, primera: null, muestras: [] }
}

export function promediar(muestras: Muestra[]): Muestra {
  const acumulado: Muestra = {}
  for (const muestra of muestras) {
    for (const [nombre, valor] of Object.entries(muestra)) {
      acumulado[nombre] = (acumulado[nombre] ?? 0) + valor
    }
  }
  const referencia: Muestra = {}
  for (const [nombre, valor] of Object.entries(acumulado)) referencia[nombre] = valor / muestras.length
  return referencia
}

function cerrar(estado: Calibracion, lista: boolean): PasoCalibracion {
  if (!lista || estado.muestras.length === 0) return { estado, lista: false }
  return { estado, lista: true, referencia: promediar(estado.muestras) }
}

export function observarCara(estado: Calibracion, muestra: Muestra, ahora: number): PasoCalibracion {
  const primera = estado.primera ?? ahora
  const siguiente: Calibracion = { ...estado, primera, muestras: [...estado.muestras, muestra] }
  const lista = siguiente.muestras.length >= MUESTRAS && ahora - primera >= ESPERA_MS
  return cerrar(siguiente, lista)
}

export function pulsoCalibracion(estado: Calibracion, ahora: number): PasoCalibracion {
  if (estado.primera == null) return { estado, lista: false }
  const lista = ahora - estado.primera >= TOPE_MS && estado.muestras.length > 0
  return cerrar(estado, lista)
}
