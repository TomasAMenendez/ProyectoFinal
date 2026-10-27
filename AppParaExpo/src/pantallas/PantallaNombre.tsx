import { useEffect, useRef, useState } from 'react'
import { APERTURA, urlVoz } from '../audio/frases'
import { reproducir } from '../audio/reproductor'
import { BotonGrande } from '../componentes/BotonGrande'
import { LogoTeo } from '../componentes/LogoTeo'
import { nombreValido } from '../juegos/comun/visita'

export function PantallaNombre({ onConfirmar }: { onConfirmar: (nombre: string) => void }) {
  const [texto, setTexto] = useState('')
  const dicha = useRef(false)
  const nombre = nombreValido(texto)

  function decirApertura(): () => void {
    if (dicha.current) return () => {}
    dicha.current = true
    return reproducir(urlVoz(APERTURA))
  }

  useEffect(() => {
    const cancelar = decirApertura()
    return () => {
      cancelar()
      dicha.current = false
    }
  }, [])

  return (
    <section className="pantalla presentacion" onPointerDown={decirApertura}>
      <LogoTeo />
      <h1>¿Cómo te llamás?</h1>
      <form
        className="formulario-nombre"
        onSubmit={(event) => {
          event.preventDefault()
          if (nombre) onConfirmar(nombre)
        }}
      >
        <input
          value={texto}
          maxLength={48}
          autoComplete="off"
          enterKeyHint="done"
          aria-label="Tu nombre"
          placeholder="Escribí tu nombre"
          onChange={(event) => setTexto(event.target.value)}
        />
        <BotonGrande type="submit" tono="sol" disabled={!nombre}>
          Jugar
        </BotonGrande>
      </form>
    </section>
  )
}
