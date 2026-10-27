import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { fraseDerrota, fraseMenu, fraseRejuego, fraseVictoria, urlVoz } from '../audio/frases'
import { limpiarVoces, reproducir } from '../audio/reproductor'
import type { ResultadoPartida } from '../juegos/comun/tipos'
import { BotonGrande } from './BotonGrande'
import '../estilos/cierre.css'

interface Props {
  resultado: ResultadoPartida | null
  onRejugar: () => void
  onMenu: () => void
  children?: ReactNode
  mensaje?: string
  conVoz?: boolean
}

export function Cierre({ resultado, onRejugar, onMenu, children, mensaje, conVoz = true }: Props) {
  useEffect(() => {
    if (!resultado || !conVoz) return
    return reproducir(urlVoz(resultado === 'gano' ? fraseVictoria() : fraseDerrota()))
  }, [resultado, conVoz])

  if (!resultado) return null
  const gano = resultado === 'gano'
  return (
    <div className={gano ? 'cierre cierre-gano' : 'cierre cierre-perdio'} role="dialog" aria-modal="true">
      {gano && <Confeti />}
      <div className="cierre-tarjeta">
        {children}
        <p className="cierre-mensaje">
          {mensaje ?? (gano ? '¡Muy bien! ¡Ganaste!' : '¡Se terminó el juego! ¡Intentalo otra vez!')}
        </p>
        <BotonGrande
          tono="hoja"
          onClick={() => {
            reproducir(urlVoz(fraseRejuego()))
            onRejugar()
          }}
        >
          VOLVER A JUGAR
        </BotonGrande>
        <BotonGrande
          tono="cielo"
          onClick={() => {
            limpiarVoces()
            reproducir(urlVoz(fraseMenu()))
            onMenu()
          }}
        >
          VOLVER AL MENÚ PRINCIPAL
        </BotonGrande>
      </div>
    </div>
  )
}

function Confeti() {
  return (
    <div className="confeti" aria-hidden="true">
      {Array.from({ length: 28 }, (_, indice) => (
        <span key={indice} style={{ left: `${(indice * 17) % 100}%`, animationDelay: `${(indice % 7) * 0.08}s` }} />
      ))}
    </div>
  )
}
