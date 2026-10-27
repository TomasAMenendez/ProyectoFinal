import { useEffect, useRef, useState } from 'react'
import { AVISO_PISTA, bienvenida, fraseAcierto, fraseError, fraseMenu, urlVoz } from '../audio/frases'
import { reproducir } from '../audio/reproductor'
import { AvisoPista } from '../componentes/AvisoPista'
import { Cierre } from '../componentes/Cierre'
import { MarcoJuego } from '../componentes/MarcoJuego'
import { fichasSombra } from '../datos/catalogos'
import { aceptarToque } from '../juegos/comun/toque'
import type { ResultadoPartida } from '../juegos/comun/tipos'
import { corazonQueSeRompe, type LadoCorazon } from '../juegos/comun/vidas'
import { LineasUnion, type Segmento } from '../juegos/sombras/LineasUnion'
import { Tablero } from '../juegos/sombras/Tablero'
import { crearSombras, unirSombras, type EstadoSombras } from '../juegos/sombras/partida'
import { useUnion } from '../juegos/sombras/useUnion'
import '../estilos/sombras.css'

let evitadas: string[] | null = null

function medir(tablero: HTMLElement): Record<string, { x: number; y: number }> {
  const base = tablero.getBoundingClientRect()
  const puntos: Record<string, { x: number; y: number }> = {}
  tablero.querySelectorAll<HTMLElement>('[data-silueta-id], [data-nombre-id]').forEach((elemento) => {
    const caja = elemento.getBoundingClientRect()
    const y = caja.top + caja.height / 2 - base.top
    if (elemento.dataset.siluetaId) {
      puntos[`s:${elemento.dataset.siluetaId}`] = { x: caja.right - base.left - 14, y }
    }
    if (elemento.dataset.nombreId) {
      puntos[`n:${elemento.dataset.nombreId}`] = { x: caja.left - base.left + 14, y }
    }
  })
  return puntos
}

export function PantallaSombras({
  onMenu,
  onRejugar,
  onTerminar,
}: {
  onMenu: () => void
  onRejugar: () => void
  onTerminar: (resultado: ResultadoPartida) => void
}) {
  const [estado, setEstado] = useState<EstadoSombras>(() => {
    const creada = crearSombras(fichasSombra(), evitadas, Math.random)
    evitadas = creada.fichas.map((ficha) => ficha.id)
    return creada
  })
  const [rompiendo, setRompiendo] = useState<LadoCorazon | null>(null)
  const [rota, setRota] = useState<Segmento | null>(null)
  const [puntos, setPuntos] = useState<Record<string, { x: number; y: number }>>({})
  const ultimo = useRef(0)
  const avisado = useRef(false)
  const estadoRef = useRef(estado)
  estadoRef.current = estado

  const union = useUnion((siluetaId, nombreId) => conectar(siluetaId, nombreId))

  useEffect(() => {
    return reproducir(urlVoz(bienvenida('sombras')))
  }, [])

  useEffect(() => {
    if (!estado.resultado || avisado.current) return
    avisado.current = true
    onTerminar(estado.resultado)
  }, [estado.resultado, onTerminar])

  useEffect(() => {
    const tablero = union.tableroRef.current
    if (!tablero) return
    const actualizar = () => setPuntos(medir(tablero))
    actualizar()
    const observador = new ResizeObserver(actualizar)
    observador.observe(tablero)
    return () => observador.disconnect()
  }, [estado.uniones, estado.fichas, union.tableroRef])

  function conectar(siluetaId: string, nombreId: string) {
    const ahora = Date.now()
    if (!aceptarToque(ultimo.current, ahora)) return
    ultimo.current = ahora
    const actual = estadoRef.current
    const paso = unirSombras(actual, siluetaId, nombreId)
    if (paso.efecto.tipo === 'ignorada') return
    const nombre = actual.nombres.find((item) => item.id === nombreId)?.nombre ?? ''
    reproducir(urlVoz(nombre))
    if (paso.efecto.tipo === 'incorrecta') {
      reproducir(urlVoz(fraseError()))
      reproducir(urlVoz(AVISO_PISTA))
      if (paso.efecto.pista) reproducir(urlVoz(paso.efecto.pista))
      setRompiendo(corazonQueSeRompe(actual.vidas))
      const origen = puntos[`s:${siluetaId}`]
      const destino = puntos[`n:${nombreId}`]
      if (origen && destino) {
        setRota({ id: 'rota', x1: origen.x, y1: origen.y, x2: destino.x, y2: destino.y })
        window.setTimeout(() => setRota(null), 480)
      }
      window.setTimeout(() => setRompiendo(null), 700)
    }
    if (paso.efecto.tipo === 'correcta') {
      reproducir(urlVoz(fraseAcierto()))
      union.setSeleccion(null)
    }
    setEstado(paso.estado)
  }

  const permanentes: Segmento[] = estado.uniones.flatMap((id) => {
    const origen = puntos[`s:${id}`]
    const destino = puntos[`n:${id}`]
    if (!origen || !destino) return []
    return [{ id, x1: origen.x, y1: origen.y, x2: destino.x, y2: destino.y }]
  })

  return (
    <MarcoJuego
      titulo="Sombras"
      vidas={estado.vidas}
      rompiendo={rompiendo}
      onMenu={() => {
        reproducir(urlVoz(fraseMenu()))
        onMenu()
      }}
    >
      {estado.aviso && !estado.resultado ? <AvisoPista pista={estado.aviso.pista} /> : <div className="aviso-reserva" />}
      <div className="tablero-sombras" ref={union.tableroRef}>
        <LineasUnion permanentes={permanentes} temporal={union.temporal} rota={rota} />
        <Tablero
          fichas={estado.fichas}
          nombres={estado.nombres}
          uniones={estado.uniones}
          seleccion={union.seleccion}
          onBajar={union.alBajar}
          onNombre={union.alElegirNombre}
        />
      </div>
      <Cierre resultado={estado.resultado} onRejugar={onRejugar} onMenu={onMenu} />
    </MarcoJuego>
  )
}
