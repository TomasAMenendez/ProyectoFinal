import { useEffect, useRef, useState } from 'react'
import { bienvenida, fraseAcierto, fraseDescartada, fraseError, fraseMenu, numeroHablado, urlVoz } from '../audio/frases'
import { reproducir } from '../audio/reproductor'
import { BotonGrande } from '../componentes/BotonGrande'
import { Cierre } from '../componentes/Cierre'
import { MarcoJuego } from '../componentes/MarcoJuego'
import { ejerciciosContar, urlImagen } from '../datos/catalogos'
import { aceptarToque } from '../juegos/comun/toque'
import type { ResultadoPartida } from '../juegos/comun/tipos'
import { corazonQueSeRompe, type LadoCorazon } from '../juegos/comun/vidas'
import { crearContar, elegirNumero, type EstadoContar } from '../juegos/contar/partida'
import '../estilos/contar.css'

let evitada: string | null = null

export function PantallaContar({
  onMenu,
  onRejugar,
  onTerminar,
}: {
  onMenu: () => void
  onRejugar: () => void
  onTerminar: (resultado: ResultadoPartida) => void
}) {
  const [estado, setEstado] = useState<EstadoContar>(() => {
    const creada = crearContar(ejerciciosContar(), evitada, Math.random)
    evitada = creada.ejercicio.id
    return creada
  })
  const [rompiendo, setRompiendo] = useState<LadoCorazon | null>(null)
  const ultimo = useRef(0)
  const avisado = useRef(false)

  useEffect(() => {
    const cancelarBienvenida = reproducir(urlVoz(bienvenida('contar')))
    const cancelarPregunta = reproducir(urlVoz(estado.ejercicio.pregunta))
    return () => {
      cancelarBienvenida()
      cancelarPregunta()
    }
  }, [estado.ejercicio.pregunta])

  useEffect(() => {
    if (!estado.resultado || avisado.current) return
    avisado.current = true
    onTerminar(estado.resultado)
  }, [estado.resultado, onTerminar])

  function elegir(numero: number) {
    const ahora = Date.now()
    if (!aceptarToque(ultimo.current, ahora)) return
    ultimo.current = ahora
    const paso = elegirNumero(estado, numero)
    if (paso.efecto.tipo === 'ignorada') return
    if (paso.efecto.tipo === 'repetida') {
      reproducir(urlVoz(fraseDescartada()))
      return
    }
    reproducir(urlVoz(numeroHablado(numero)))
    if (paso.efecto.tipo === 'correcta') reproducir(urlVoz(fraseAcierto()))
    if (paso.efecto.tipo === 'incorrecta') {
      reproducir(urlVoz(fraseError()))
      setRompiendo(corazonQueSeRompe(estado.vidas))
      window.setTimeout(() => setRompiendo(null), 700)
    }
    setEstado(paso.estado)
  }

  return (
    <MarcoJuego
      titulo="Contar"
      vidas={estado.vidas}
      rompiendo={rompiendo}
      onMenu={() => {
        reproducir(urlVoz(fraseMenu()))
        onMenu()
      }}
    >
      <img className="imagen-contar" src={urlImagen(estado.ejercicio.imagen)} alt="" />
      <p className="pregunta-contar">{estado.ejercicio.pregunta}</p>
      <div className="numeros-contar">
        {estado.opciones.map((numero) => {
          const descartada = estado.descartadas.includes(numero)
          return (
            <BotonGrande key={numero} tono="sol" className={descartada ? 'opcion-apagada' : ''} onClick={() => elegir(numero)}>
              {numero}
            </BotonGrande>
          )
        })}
      </div>
      <Cierre resultado={estado.resultado} onRejugar={onRejugar} onMenu={onMenu} />
    </MarcoJuego>
  )
}
