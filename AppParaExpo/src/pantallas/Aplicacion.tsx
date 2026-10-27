import { useCallback, useState } from 'react'
import { VAMOS_A_JUGAR, urlVoz } from '../audio/frases'
import { reproducirVeces } from '../audio/reproductor'
import { anotaEnPlanilla } from '../juegos/ppt/planilla'
import { configDeCompilacion, fechaLocalIso, registrarResultado } from '../servicios/registro'
import type { JuegoId, ResultadoPartida } from '../juegos/comun/tipos'
import { abrirJuego, confirmarNombre, visitaInicial, volverAlMenu } from '../juegos/comun/visita'
import { PantallaCuentos, cuentosVisibles } from '@cuentos'
import { PantallaAdivina } from './PantallaAdivina'
import { PantallaContar } from './PantallaContar'
import { PantallaEspejito } from './PantallaEspejito'
import { PantallaMenu } from './PantallaMenu'
import { PantallaPpt } from './PantallaPpt'
import { PantallaNombre } from './PantallaNombre'
import { PantallaSombras } from './PantallaSombras'

export function Aplicacion() {
  const [visita, setVisita] = useState(visitaInicial)
  const [ronda, setRonda] = useState(0)

  const terminar = useCallback(
    (juego: JuegoId, resultado: ResultadoPartida) => {
      if (!visita.nombre || !anotaEnPlanilla(juego)) return
      void registrarResultado(
        { nombre: visita.nombre, juego, resultado, fechaHora: fechaLocalIso() },
        configDeCompilacion(),
      )
    },
    [visita.nombre],
  )

  const alMenu = useCallback(() => setVisita((actual) => volverAlMenu(actual)), [])
  const alRejugar = useCallback(() => setRonda((valor) => valor + 1), [])

  if (visita.pantalla === 'nombre' || !visita.nombre) {
    return (
      <PantallaNombre
        onConfirmar={(nombre) => {
          setVisita(confirmarNombre(visitaInicial(), nombre))
          reproducirVeces(urlVoz(VAMOS_A_JUGAR), 2)
        }}
      />
    )
  }
  if (visita.pantalla === 'menu') {
    return (
      <PantallaMenu
        nombre={visita.nombre}
        onElegir={(juego) => {
          setRonda((valor) => valor + 1)
          setVisita((actual) => abrirJuego(actual, juego))
        }}
      />
    )
  }

  if (visita.pantalla === 'cuentos' && cuentosVisibles) return <PantallaCuentos onMenu={alMenu} />

  const props = {
    onMenu: alMenu,
    onRejugar: alRejugar,
    onTerminar: (resultado: ResultadoPartida) => terminar(visita.pantalla as JuegoId, resultado),
  }
  if (visita.pantalla === 'sombras') return <PantallaSombras key={ronda} {...props} />
  if (visita.pantalla === 'adivina') return <PantallaAdivina key={ronda} {...props} />
  if (visita.pantalla === 'ppt') return <PantallaPpt key={ronda} onMenu={alMenu} onTerminar={props.onTerminar} />
  if (visita.pantalla === 'espejito') return <PantallaEspejito key={ronda} onMenu={alMenu} onTerminar={props.onTerminar} />
  return <PantallaContar key={ronda} {...props} />
}
