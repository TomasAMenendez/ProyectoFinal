import type { PointerEvent as ReactPointerEvent } from 'react'
import { urlImagen } from '../../datos/catalogos'
import type { FichaSombra } from '../comun/tipos'

interface Nombre {
  id: string
  nombre: string
}

interface Props {
  fichas: FichaSombra[]
  nombres: Nombre[]
  uniones: string[]
  seleccion: string | null
  onBajar: (id: string, event: ReactPointerEvent<HTMLButtonElement>) => void
  onNombre: (id: string) => void
}

export function Tablero({ fichas, nombres, uniones, seleccion, onBajar, onNombre }: Props) {
  return (
    <>
      <div className="columna-siluetas">
        {fichas.map((ficha) => {
          const resuelto = uniones.includes(ficha.id)
          return (
            <button
              key={ficha.id}
              type="button"
              className={seleccion === ficha.id ? 'pieza pieza-activa' : 'pieza'}
              data-silueta-id={ficha.id}
              data-resuelto={resuelto ? 'si' : 'no'}
              disabled={resuelto}
              onPointerDown={(event) => onBajar(ficha.id, event)}
            >
              <img src={urlImagen(ficha.silueta)} alt="" />
            </button>
          )
        })}
      </div>
      <div className="columna-nombres">
        {nombres.map((nombre) => {
          const resuelto = uniones.includes(nombre.id)
          return (
            <button
              key={nombre.id}
              type="button"
              className="pieza pieza-nombre"
              data-nombre-id={nombre.id}
              data-resuelto={resuelto ? 'si' : 'no'}
              disabled={resuelto}
              onClick={() => onNombre(nombre.id)}
            >
              {nombre.nombre}
            </button>
          )
        })}
      </div>
    </>
  )
}
