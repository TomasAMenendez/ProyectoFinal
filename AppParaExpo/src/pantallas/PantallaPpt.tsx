import { useEffect, useRef, useState } from 'react'
import { bienvenida, fraseMenu, fraseRejuego, numeroHablado, urlVoz } from '../audio/frases'
import { limpiarVoces, reproducir } from '../audio/reproductor'
import { BotonGrande } from '../componentes/BotonGrande'
import { LogoTeo } from '../componentes/LogoTeo'
import { urlImagen } from '../datos/catalogos'
import { jugarSiLista, type CierreRonda } from '../juegos/ppt/azar'
import {
  avanzarTramo,
  iniciarCuenta,
  vigilarCuenta,
  type Cuenta,
  type Tramo,
} from '../juegos/ppt/estabilidad'
import { FIGURAS, figuraPorId, type FiguraId } from '../juegos/ppt/figuras'
import { mensajeDe, type MotivoCamara } from '../juegos/ppt/mensajes'
import type { ManoVista } from '../juegos/ppt/reconocedor'
import { textoResultado } from '../juegos/ppt/reglas'
import { useCamara } from '../juegos/ppt/useCamara'
import type { ResultadoPartida } from '../juegos/comun/tipos'
import '../estilos/ppt.css'

interface Props {
  onMenu: () => void
  onTerminar: (resultado: ResultadoPartida) => void
}

const vacio: Tramo = { figura: null, desde: null }

export function PantallaPpt({ onMenu, onTerminar }: Props) {
  const [modo, setModo] = useState<'pendiente' | 'camara' | 'botones'>('pendiente')
  const [motivo, setMotivo] = useState<MotivoCamara | null>(null)
  const [reintento, setReintento] = useState(0)
  const [aviso, setAviso] = useState('¡Prepará tu mano!')
  const [detalle, setDetalle] = useState('Mostrá uno de estos tres gestos para jugar.')
  const [cuenta, setCuenta] = useState<Cuenta | null>(null)
  const [cierre, setCierre] = useState<CierreRonda | null>(null)
  const tramo = useRef<Tramo>(vacio)
  const cuentaRef = useRef<Cuenta | null>(null)
  const cierreRef = useRef<CierreRonda | null>(null)
  const avisado = useRef(false)
  cierreRef.current = cierre

  useEffect(() => reproducir(urlVoz(bienvenida('ppt'))), [])

  useEffect(() => {
    if (!cuenta) return
    return reproducir(urlVoz(numeroHablado(cuenta.numero)))
  }, [cuenta])

  useEffect(() => {
    if (!cierre || avisado.current) return
    avisado.current = true
    onTerminar(cierre.resultado)
    return reproducir(urlVoz(textoResultado(cierre.resultado)))
  }, [cierre, onTerminar])

  const fijar = (figura: FiguraId) => {
    const ronda = jugarSiLista(figura, Math.random)
    cuentaRef.current = null
    tramo.current = vacio
    setCuenta(null)
    setCierre(ronda)
  }

  const otra = () => {
    avisado.current = false
    cuentaRef.current = null
    tramo.current = vacio
    setCuenta(null)
    setCierre(null)
    setAviso('¡Prepará tu mano!')
    setDetalle('Mostrá uno de estos tres gestos para jugar.')
  }

  const alMano = (mano: ManoVista, momento: number) => {
    if (cierreRef.current) return
    const obs = { figura: mano.figura, confianza: mano.confianza, momento }
    if (cuentaRef.current) {
      const paso = vigilarCuenta(cuentaRef.current, obs)
      if (paso.estado === 'cancelada') {
        cuentaRef.current = null
        tramo.current = vacio
        setCuenta(null)
        setAviso('¡Prepará tu mano!')
        setDetalle('Mostrá uno de estos tres gestos para jugar.')
        return
      }
      if (paso.estado === 'lista') {
        fijar(cuentaRef.current.figura)
        return
      }
      cuentaRef.current = paso.cuenta
      setCuenta(paso.cuenta)
      return
    }
    const paso = avanzarTramo(tramo.current, obs)
    tramo.current = paso.tramo
    if (paso.confirmado && paso.tramo.figura) {
      const nueva = iniciarCuenta(paso.tramo.figura, momento)
      cuentaRef.current = nueva
      setCuenta(nueva)
      setAviso('¡Te detecté! Mantené el gesto...')
      setDetalle('Mostrá uno de estos tres gestos para jugar.')
      return
    }
    if (!mano.hayMano) {
      setAviso('Acercá tu mano a la cámara')
      return
    }
    if (!paso.tramo.figura) {
      setAviso('No reconozco ese gesto. Probá con piedra, papel o tijera.')
    }
  }

  const { videoRef, lista } = useCamara({
    activa: modo === 'camara' && motivo === null,
    onMano: alMano,
    onError: (fallo) => {
      setModo('pendiente')
      setMotivo(fallo)
      cuentaRef.current = null
      tramo.current = vacio
      setCuenta(null)
    },
  })

  const activar = () => {
    setMotivo(null)
    setCierre(null)
    setReintento((valor) => valor + 1)
    setModo('camara')
  }

  const sinCamara = () => {
    setMotivo(null)
    setModo('botones')
  }

  return (
    <section className="pantalla juego pantalla-ppt">
      <header className="barra-juego">
        <LogoTeo compacto />
        <h1>Piedra, papel o tijera</h1>
        <BotonGrande
          tono="tinta"
          onClick={() => {
            reproducir(urlVoz(fraseMenu()))
            onMenu()
          }}
        >
          Menú
        </BotonGrande>
      </header>
      <div className={modo === 'botones' && !cierre ? 'escenario-ppt sin-espejo' : 'escenario-ppt'}>
        <div className={cierre ? 'zona-ppt con-resultado' : 'zona-ppt'}>
          {cierre ? (
            <div className={`resultado-ppt resultado-${cierre.resultado}`}>
              <div>
                <p>Vos</p>
                <img src={urlImagen(figuraPorId(cierre.persona).imagen)} alt="" />
                <strong>{figuraPorId(cierre.persona).nombre}</strong>
              </div>
              <div>
                <p>TEO</p>
                <img src={urlImagen(figuraPorId(cierre.computadora).imagen)} alt="" />
                <strong>{figuraPorId(cierre.computadora).nombre}</strong>
              </div>
              <p className="mensaje-resultado">{textoResultado(cierre.resultado)}</p>
            </div>
          ) : (
            <>
              <p className="aviso-ppt">{aviso}</p>
              <p className="detalle-ppt">{detalle}</p>
            </>
          )}
          {modo === 'camara' && motivo === null ? (
            <div className={cierre ? 'camara-ppt camara-en-espera' : 'camara-ppt'} key={reintento}>
              <video ref={videoRef} autoPlay muted playsInline />
              {cierre ? null : (
                <div className="guia-mano">
                  <span>Dejá la mano dentro de este recuadro</span>
                </div>
              )}
              {cuenta && !cierre ? <p className="cuenta-ppt">{cuenta.numero}</p> : null}
              {lista || cierre ? null : <p className="espera-ppt">Preparando la cámara...</p>}
            </div>
          ) : null}
          {motivo ? <p className="falla-ppt">{mensajeDe(motivo)}</p> : null}
          {modo === 'pendiente' && !cierre ? (
            <div className="acciones-ppt">
              <BotonGrande tono="cielo" onClick={activar}>
                Activar cámara
              </BotonGrande>
              <BotonGrande tono="hoja" onClick={sinCamara}>
                Continuar sin cámara
              </BotonGrande>
            </div>
          ) : null}
          {cierre ? (
            <div className="acciones-ppt">
              <BotonGrande
                tono="hoja"
                onClick={() => {
                  reproducir(urlVoz(fraseRejuego()))
                  otra()
                }}
              >
                Jugar otra vez
              </BotonGrande>
              <BotonGrande
                tono="tinta"
                onClick={() => {
                  limpiarVoces()
                  reproducir(urlVoz(fraseMenu()))
                  onMenu()
                }}
              >
                Volver al menú principal
              </BotonGrande>
            </div>
          ) : null}
        </div>
        {modo === 'pendiente' || cierre ? null : (
          <div className="figuras-ppt">
            {FIGURAS.map((figura) => (
              <button key={figura.id} type="button" className="figura-ppt" onClick={() => fijar(figura.id)}>
                <img src={urlImagen(figura.imagen)} alt="" />
                <span>{figura.nombre}</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}
