import type { ReactNode } from 'react'
import type { LadoCorazon } from '../juegos/comun/vidas'
import { BotonGrande } from './BotonGrande'
import { LogoTeo } from './LogoTeo'
import { Vidas } from './Vidas'

interface Props {
  titulo: string
  vidas: number
  rompiendo: LadoCorazon | null
  onMenu: () => void
  children: ReactNode
}

export function MarcoJuego({ titulo, vidas, rompiendo, onMenu, children }: Props) {
  return (
    <section className="pantalla juego">
      <header className="barra-juego">
        <LogoTeo compacto />
        <h1>{titulo}</h1>
        <Vidas vidas={vidas} rompiendo={rompiendo} />
        <BotonGrande tono="tinta" onClick={onMenu}>
          Menú
        </BotonGrande>
      </header>
      {children}
    </section>
  )
}
