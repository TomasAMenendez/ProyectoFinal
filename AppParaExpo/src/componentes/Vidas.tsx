import type { LadoCorazon } from '../juegos/comun/vidas'
import '../estilos/vidas.css'

const LADOS: LadoCorazon[] = ['izquierda', 'medio', 'derecha']
const UMBRAL: Record<LadoCorazon, number> = { derecha: 2, medio: 1, izquierda: 0 }

export function Vidas({ vidas, rompiendo }: { vidas: number; rompiendo: LadoCorazon | null }) {
  return (
    <div className="vidas" aria-label={`${vidas} vidas`}>
      {LADOS.map((lado) => {
        const estado = rompiendo === lado ? 'rompiendo' : vidas <= UMBRAL[lado] ? 'vacio' : 'lleno'
        return (
          <span key={lado} className={`corazon corazon-${estado}`} aria-hidden="true">
            <svg viewBox="0 0 64 64">
              <path d="M32 54 12 34C4 26 6 14 16 12c6-1 10 3 16 10 6-7 10-11 16-10 10 2 12 14 4 22Z" />
            </svg>
          </span>
        )
      })}
    </div>
  )
}
