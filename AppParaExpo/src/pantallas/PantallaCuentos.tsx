import { useEffect, useMemo, useRef, useState } from 'react'
import { fraseMenu, urlVoz } from '../audio/frases'
import { limpiarVoces, reproducir } from '../audio/reproductor'
import { urlImagen } from '../datos/catalogos'
import { CUENTOS, type Cuento } from '../juegos/cuentos/catalogo'
import { indiceEn, planDeLectura, ventanaDe } from '../juegos/cuentos/lectura'
import '../estilos/cuentos.css'

export function PantallaCuentos({ onMenu }: { onMenu: () => void }) {
  const [elegido, setElegido] = useState<string | null>(null)
  const cuento = CUENTOS.find((item) => item.id === elegido) ?? null

  const alMenu = () => {
    limpiarVoces()
    reproducir(urlVoz(fraseMenu()))
    onMenu()
  }

  return (
    <section className="pantalla juego">
      {cuento ? (
        <Lectura key={cuento.id} cuento={cuento} onVolver={() => setElegido(null)} onMenu={alMenu} />
      ) : (
        <>
          <header className="barra-juego">
            <h1>Cuentos</h1>
            <button type="button" className="boton-grande tono-cielo" onClick={alMenu}>
              Menú
            </button>
          </header>
          <Lista onElegir={setElegido} />
        </>
      )}
    </section>
  )
}

function Lista({ onElegir }: { onElegir: (id: string) => void }) {
  return (
    <div className="elige-cuento">
      {CUENTOS.map((cuento) => (
        <button
          key={cuento.id}
          type="button"
          className={`tarjeta-juego cuento-${cuento.id}`}
          onClick={() => onElegir(cuento.id)}
        >
          <img src={urlImagen(cuento.imagen)} alt="" />
          <span>{cuento.titulo}</span>
        </button>
      ))}
    </div>
  )
}

function Lectura({ cuento, onVolver, onMenu }: { cuento: Cuento; onVolver: () => void; onMenu: () => void }) {
  const plan = useMemo(() => planDeLectura(cuento.texto), [cuento])
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const [progreso, setProgreso] = useState(0)
  const [pausado, setPausado] = useState(false)
  const [pedirPlay, setPedirPlay] = useState(false)
  const ventana = ventanaDe(plan, indiceEn(plan, progreso), 6)

  useEffect(() => {
    limpiarVoces()
    const audio = new Audio(cuento.audio)
    audioRef.current = audio
    const marcar = () => {
      if (!Number.isFinite(audio.duration) || audio.duration <= 0) return
      setProgreso(audio.currentTime / audio.duration)
    }
    audio.addEventListener('timeupdate', marcar)
    audio.addEventListener('ended', () => {
      setProgreso(1)
      setPausado(true)
    })
    void audio.play().catch(() => setPedirPlay(true))
    return () => {
      audio.pause()
      audioRef.current = null
    }
  }, [cuento])

  const alternar = () => {
    const audio = audioRef.current
    if (!audio) return
    if (audio.paused) {
      if (audio.ended) audio.currentTime = 0
      void audio.play().then(() => setPausado(false)).catch(() => setPedirPlay(true))
      return
    }
    audio.pause()
    setPausado(true)
  }

  return (
    <>
      <header className="barra-juego">
        <h1>{cuento.titulo}</h1>
        <button type="button" className="boton-grande tono-tinta" onClick={onVolver}>
          Cuentos
        </button>
        <button type="button" className="boton-grande tono-sol" onClick={alternar}>
          {pedirPlay || pausado ? 'Seguir' : 'Pausa'}
        </button>
        <button type="button" className="boton-grande tono-cielo" onClick={onMenu}>
          Menú
        </button>
      </header>
      <article className="lectura">
        <div className="lectura-cuerpo">
          {ventana.map((trozo) => (
            <p key={trozo.indice} className={trozo.activo ? 'lectura-activa' : 'lectura-espera'}>
              {trozo.texto}
            </p>
          ))}
        </div>
        <div className="lectura-riel">
          <div className="lectura-avance" style={{ width: `${Math.round(progreso * 100)}%` }} />
        </div>
      </article>
    </>
  )
}
