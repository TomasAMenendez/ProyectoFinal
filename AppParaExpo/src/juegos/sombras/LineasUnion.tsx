export interface Segmento {
  id: string
  x1: number
  y1: number
  x2: number
  y2: number
}

interface Props {
  permanentes: Segmento[]
  temporal: Segmento | null
  rota: Segmento | null
}

export function LineasUnion({ permanentes, temporal, rota }: Props) {
  return (
    <svg className="lineas" aria-hidden="true">
      {permanentes.map((linea) => (
        <line key={linea.id} className="linea-ok" x1={linea.x1} y1={linea.y1} x2={linea.x2} y2={linea.y2} />
      ))}
      {temporal && <line className="linea-temp" x1={temporal.x1} y1={temporal.y1} x2={temporal.x2} y2={temporal.y2} />}
      {rota && <line className="linea-rota" x1={rota.x1} y1={rota.y1} x2={rota.x2} y2={rota.y2} />}
    </svg>
  )
}
