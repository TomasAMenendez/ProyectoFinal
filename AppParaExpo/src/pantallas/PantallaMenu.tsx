import { cuentosVisibles } from '@cuentos'
import { enlaceVestir } from '@enlace-vestir'
import { ejerciciosContar, fichasAdivina, fichasSombra, urlImagen } from '../datos/catalogos'
import type { JuegoId } from '../juegos/comun/tipos'
import { LogoTeo } from '../componentes/LogoTeo'
import '../estilos/menu.css'

const TARJETAS: { id: JuegoId; titulo: string; imagen: string }[] = [
  { id: 'sombras', titulo: 'SOMBRAS', imagen: fichasSombra()[0]?.silueta ?? '' },
  { id: 'adivina', titulo: 'ADIVINA EL OBJETO', imagen: fichasAdivina()[2]?.imagen ?? '' },
  { id: 'contar', titulo: 'CONTAR', imagen: ejerciciosContar()[1]?.imagen ?? '' },
  { id: 'ppt', titulo: 'PIEDRA, PAPEL O TIJERA', imagen: 'Imagen_Tijera.png' },
  { id: 'espejito', titulo: 'ESPEJITO', imagen: 'Emocion_Feliz.png' },
]

export function PantallaMenu({ nombre, onElegir }: { nombre: string; onElegir: (juego: JuegoId | 'cuentos') => void }) {
  return (
    <section className="pantalla presentacion menu">
      <LogoTeo compacto />
      <h1>Hola, {nombre}</h1>
      <p className="subtitulo">Elegí un juego</p>
      <div className={cuentosVisibles ? 'menu-juegos con-cuentos' : 'menu-juegos'}>
        <div className={cuentosVisibles ? 'rejilla-menu con-cuentos' : 'rejilla-menu'}>
          {TARJETAS.map((tarjeta) => (
            <button key={tarjeta.id} type="button" className={`tarjeta-juego tarjeta-${tarjeta.id}`} onClick={() => onElegir(tarjeta.id)}>
              <img src={urlImagen(tarjeta.imagen)} alt="" />
              <span>{tarjeta.titulo}</span>
            </button>
          ))}
          {cuentosVisibles ? (
            <button type="button" className="tarjeta-juego tarjeta-cuentos" onClick={() => onElegir('cuentos')}>
              <img src={urlImagen('Imagen_Luna.png')} alt="" />
              <span>Cuentos</span>
            </button>
          ) : null}
        </div>
        {enlaceVestir ? (
          <a className="vesti-teo" href={enlaceVestir}>
            Vesti a teo
          </a>
        ) : null}
      </div>
    </section>
  )
}
