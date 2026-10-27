import { useEffect, useRef, useState } from 'react'
import { bienvenida, fraseAcierto, fraseDescartada, fraseError, fraseMenu, urlVoz } from '../audio/frases'
import { reproducir } from '../audio/reproductor'
import { BotonGrande } from '../componentes/BotonGrande'
import { Cierre } from '../componentes/Cierre'
import { MarcoJuego } from '../componentes/MarcoJuego'
import { fichasAdivina, urlImagen } from '../datos/catalogos'
import { aceptarToque } from '../juegos/comun/toque'
import type { ResultadoPartida } from '../juegos/comun/tipos'
import { corazonQueSeRompe, type LadoCorazon } from '../juegos/comun/vidas'
import { crearAdivina, elegirOpcion, type EstadoAdivina } from '../juegos/adivina/partida'
import '../estilos/adivina.css'

let evitada: string | null = null

export function PantallaAdivina({
  onMenu,
  onRejugar,
  onTerminar,
}: {
  onMenu: () => void
  onRejugar: () => void
  onTerminar: (resultado: ResultadoPartida) => void
}) {
  const [estado, setEstado] = useState<EstadoAdivina>(() => {
    const creada = crearAdivina(fichasAdivina(), evitada, Math.random)
    evitada = creada.objetivo.id
    return creada
  })
  const [rompiendo, setRompiendo] = useState<LadoCorazon | null>(null)
  const ultimo = useRef(0)
  const avisado = useRef(false)

  useEffect(() => {
    const cancelarBienvenida = reproducir(urlVoz(bienvenida('adivina')))
    const primera = estado.pistasVisibles[0]
    const cancelarPista = primera ? reproducir(urlVoz(primera)) : () => {}
    return () => {
      cancelarBienvenida()
      cancelarPista()
    }
  }, [])

  useEffect(() => {
    if (!estado.resultado || avisado.current) return
    avisado.current = true
    onTerminar(estado.resultado)
  }, [estado.resultado, onTerminar])

  function elegir(nombre: string) {
    const ahora = Date.now()
    if (!aceptarToque(ultimo.current, ahora)) return
    ultimo.current = ahora
    const paso = elegirOpcion(estado, nombre)
    if (paso.efecto.tipo === 'ignorada') return
    if (paso.efecto.tipo === 'repetida') {
      reproducir(urlVoz(fraseDescartada()))
      return
    }
    reproducir(urlVoz(nombre))
    if (paso.efecto.tipo === 'correcta') reproducir(urlVoz(fraseAcierto()))
    if (paso.efecto.tipo === 'incorrecta') {
      reproducir(urlVoz(fraseError()))
      if (paso.efecto.pistaNueva) reproducir(urlVoz(paso.efecto.pistaNueva))
      setRompiendo(corazonQueSeRompe(estado.vidas))
      window.setTimeout(() => setRompiendo(null), 700)
    }
    setEstado(paso.estado)
  }

  return (
    <MarcoJuego
      titulo="Adivina el objeto"
      vidas={estado.vidas}
      rompiendo={rompiendo}
      onMenu={() => {
        reproducir(urlVoz(fraseMenu()))
        onMenu()
      }}
    >
      <div className="pistas-adivina">
        {estado.pistasVisibles.map((pista) => (
          <p key={pista}>{pista}</p>
        ))}
      </div>
      <div className="opciones-adivina">
        {estado.opciones.map((opcion) => {
          const descartada = estado.descartadas.includes(opcion)
          return (
            <BotonGrande key={opcion} tono="cielo" className={descartada ? 'opcion-apagada' : ''} onClick={() => elegir(opcion)}>
              {opcion}
            </BotonGrande>
          )
        })}
      </div>
      <Cierre resultado={estado.resultado} onRejugar={onRejugar} onMenu={onMenu}>
        {estado.resultado && (
          <img className="revelacion" src={urlImagen(estado.objetivo.imagen)} alt={estado.objetivo.nombreCorrecto} />
        )}
      </Cierre>
    </MarcoJuego>
  )
}
