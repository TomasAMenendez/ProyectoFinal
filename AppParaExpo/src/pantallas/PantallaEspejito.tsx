import { useEffect, useRef, useState } from 'react'
import { bienvenida, fraseMenu, urlVoz } from '../audio/frases'
import { limpiarVoces, reproducir } from '../audio/reproductor'
import { BotonGrande } from '../componentes/BotonGrande'
import { Cierre } from '../componentes/Cierre'
import { LogoTeo } from '../componentes/LogoTeo'
import { urlImagen } from '../datos/catalogos'
import { calibracionNueva, observarCara, pulsoCalibracion, type Calibracion, type Muestra } from '../juegos/espejito/calibracion'
import { cumple, puntuaciones } from '../juegos/espejito/clasificacion'
import { emocionPorId, type EmocionId } from '../juegos/espejito/emociones'
import { actualizarAvance, avanceInicial, confirmado, type Avance } from '../juegos/espejito/estabilidad'
import { CALIBRACION, CIERRE, mensajeAyuda, mensajeDe, PROBA, SIN_CARA, type MotivoCamara } from '../juegos/espejito/mensajes'
import { sesionNueva, siguiente, type Sesion } from '../juegos/espejito/sesion'
import { useCamara } from '../juegos/espejito/useCamara'
import type { ResultadoPartida } from '../juegos/comun/tipos'
import '../estilos/espejito.css'

interface Props {
  onMenu: () => void
  onTerminar: (resultado: ResultadoPartida) => void
}

type Fase = 'entrada' | 'preparando' | 'calibrando' | 'desafio' | 'resumen' | 'impedida'

function decir(texto: string, setFrase: (texto: string) => void, setToma: (valor: (actual: number) => number) => void) {
  setFrase(texto)
  setToma((actual) => actual + 1)
}

export function PantallaEspejito({ onMenu, onTerminar }: Props) {
  const [fase, setFase] = useState<Fase>('entrada')
  const [motivo, setMotivo] = useState<MotivoCamara | null>(null)
  const [reintento, setReintento] = useState(0)
  const [sesion, setSesion] = useState<Sesion>(() => sesionNueva(Math.random))
  const [mensaje, setMensaje] = useState('')
  const [progreso, setProgreso] = useState(0)
  const [frase, setFrase] = useState<string | null>(null)
  const [toma, setToma] = useState(0)

  const faseRef = useRef(fase)
  const sesionRef = useRef(sesion)
  const referencia = useRef<Muestra>({})
  const calibracion = useRef<Calibracion>(calibracionNueva(0))
  const avance = useRef<Avance>(avanceInicial())
  const avisado = useRef(false)
  const instruccionEn = useRef(0)
  const mensajeRef = useRef('')
  const progresoRef = useRef(0)
  faseRef.current = fase
  sesionRef.current = sesion

  const hablar = (texto: string) => decir(texto, setFrase, setToma)

  useEffect(() => {
    reproducir(urlVoz(bienvenida('espejito')))
  }, [])

  useEffect(() => {
    if (!frase) return
    return reproducir(urlVoz(frase))
  }, [frase, toma])

  useEffect(() => {
    if (fase !== 'resumen' || avisado.current) return
    avisado.current = true
    onTerminar('gano')
    hablar(CIERRE)
  }, [fase, onTerminar])

  const publicar = (texto: string, valor: number, conVoz: boolean) => {
    const cubo = Math.round(valor * 20) / 20
    if (texto !== mensajeRef.current) {
      mensajeRef.current = texto
      setMensaje(texto)
      if (conVoz && texto && performance.now() - instruccionEn.current > 1200) hablar(texto)
    }
    if (cubo !== progresoRef.current) {
      progresoRef.current = cubo
      setProgreso(cubo)
    }
  }

  const abrirDesafio = (orden: EmocionId[], indice: number) => {
    const emocion = emocionPorId(orden[indice])
    avance.current = avanceInicial()
    progresoRef.current = 0
    mensajeRef.current = emocion.instruccion
    instruccionEn.current = performance.now()
    setProgreso(0)
    setMensaje(emocion.instruccion)
    faseRef.current = 'desafio'
    setFase('desafio')
    hablar(emocion.instruccion)
  }

  const alCara = (muestra: Record<string, number> | null, ahora: number) => {
    const actual = faseRef.current
    if (actual === 'calibrando') {
      const paso = muestra
        ? observarCara(calibracion.current, muestra, ahora)
        : pulsoCalibracion(calibracion.current, ahora)
      calibracion.current = paso.estado
      if (!muestra) publicar(SIN_CARA, 0, true)
      else if (mensajeRef.current === SIN_CARA) publicar(CALIBRACION, 0, false)
      if (paso.lista && paso.referencia) {
        referencia.current = paso.referencia
        abrirDesafio(sesionRef.current.orden, sesionRef.current.indice)
      }
      return
    }
    if (actual !== 'desafio') return
    const id = sesionRef.current.orden[sesionRef.current.indice]
    if (!id) return
    if (!muestra) {
      avance.current = actualizarAvance(avance.current, false, ahora)
      publicar(mensajeAyuda(false), avance.current.progreso, true)
      return
    }
    const ok = cumple(id, puntuaciones(muestra, referencia.current))
    avance.current = actualizarAvance(avance.current, ok, ahora)
    if (confirmado(avance.current)) {
      progresoRef.current = 1
      setProgreso(1)
      const paso = siguiente(sesionRef.current)
      sesionRef.current = paso.sesion
      setSesion(paso.sesion)
      if (paso.anotar) {
        faseRef.current = 'resumen'
        setFase('resumen')
        return
      }
      abrirDesafio(paso.sesion.orden, paso.sesion.indice)
      return
    }
    publicar(ok ? emocionPorId(id).instruccion : mensajeAyuda(true), avance.current.progreso, !ok)
  }

  const camaraActiva = fase === 'preparando' || fase === 'calibrando' || fase === 'desafio' || fase === 'resumen'
  const { videoRef, lista } = useCamara({
    activa: camaraActiva,
    reintento,
    onCara: alCara,
    onError: (fallo) => {
      faseRef.current = 'impedida'
      setMotivo(fallo)
      setFase('impedida')
    },
  })

  useEffect(() => {
    if (!lista || faseRef.current !== 'preparando') return
    calibracion.current = calibracionNueva(performance.now())
    mensajeRef.current = CALIBRACION
    instruccionEn.current = performance.now()
    faseRef.current = 'calibrando'
    setMensaje(CALIBRACION)
    setFase('calibrando')
    hablar(CALIBRACION)
  }, [lista])

  const comenzar = () => {
    setMotivo(null)
    mensajeRef.current = 'Estoy preparando la cámara.'
    setMensaje('Estoy preparando la cámara.')
    setReintento((valor) => valor + 1)
    faseRef.current = 'preparando'
    setFase('preparando')
  }

  const repetir = () => {
    if (fase === 'calibrando') hablar(CALIBRACION)
    if (fase === 'desafio') hablar(emocionPorId(sesion.orden[sesion.indice]).instruccion)
  }

  const otra = () => {
    const nueva = sesionNueva(Math.random)
    avisado.current = false
    sesionRef.current = nueva
    referencia.current = {}
    calibracion.current = calibracionNueva(performance.now())
    avance.current = avanceInicial()
    progresoRef.current = 0
    mensajeRef.current = CALIBRACION
    instruccionEn.current = performance.now()
    setSesion(nueva)
    setProgreso(0)
    setMensaje(CALIBRACION)
    faseRef.current = 'calibrando'
    setFase('calibrando')
  }

  const salir = () => {
    const url = urlVoz(fraseMenu())
    if (url) reproducir(url)
    else limpiarVoces()
    onMenu()
  }

  const idVisible = fase === 'resumen' ? sesion.orden[Math.max(0, sesion.indice - 1)] : fase === 'desafio' ? sesion.orden[sesion.indice] : undefined
  const emocion = idVisible ? emocionPorId(idVisible) : null
  const logrado = fase === 'resumen'

  return (
    <section className="pantalla juego pantalla-espejito">
      <header className="barra-juego">
        <LogoTeo compacto />
        <h1>ESPEJITO</h1>
        <BotonGrande tono="tinta" onClick={salir}>
          Menú
        </BotonGrande>
      </header>

      {fase === 'entrada' ? (
        <div className="entrada-espejito">
          <p>Imitá la carita que te muestro.</p>
          <BotonGrande tono="sol" onClick={comenzar}>
            Comenzar a jugar
          </BotonGrande>
        </div>
      ) : null}

      {fase === 'impedida' && motivo ? (
        <div className="entrada-espejito">
          <p className="falla-espejito">{mensajeDe(motivo)}</p>
          <div className="acciones-espejito">
            <BotonGrande tono="sol" onClick={comenzar}>
              Reintentar
            </BotonGrande>
            <BotonGrande tono="tinta" onClick={salir}>
              Salir
            </BotonGrande>
          </div>
        </div>
      ) : null}

      {camaraActiva ? (
        <div className="escenario-espejito">
          <div className="referencia-espejito">
            {emocion ? <img src={urlImagen(emocion.imagen)} alt="" /> : null}
            {emocion ? <p className="nombre-emocion">{emocion.nombre}</p> : null}
            <p className={logrado ? 'logro-espejito' : 'instruccion-espejito'}>{logrado ? emocion?.felicitacion : mensaje}</p>
          </div>
          <div className="camara-espejito">
            <video ref={videoRef} autoPlay muted playsInline />
          </div>
        </div>
      ) : null}

      {fase === 'calibrando' || fase === 'desafio' || logrado ? (
        <div className="avance-espejito" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progreso * 100)}>
          <span style={{ width: `${Math.round(progreso * 100)}%` }} />
        </div>
      ) : null}

      {fase === 'calibrando' || fase === 'desafio' ? (
        <div className="acciones-espejito">
          <BotonGrande tono="cielo" onClick={repetir}>
            Repetir
          </BotonGrande>
        </div>
      ) : null}

      <Cierre resultado={fase === 'resumen' ? 'gano' : null} mensaje={CIERRE} conVoz={false} onRejugar={otra} onMenu={onMenu} />
    </section>
  )
}
