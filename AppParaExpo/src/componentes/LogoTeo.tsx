import { urlImagen } from '../datos/catalogos'

export function LogoTeo({ compacto = false }: { compacto?: boolean }) {
  return <img className={compacto ? 'logo-teo logo-compacto' : 'logo-teo'} src={urlImagen('logo-teo.png')} alt="TEO" />
}
