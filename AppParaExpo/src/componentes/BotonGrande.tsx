import type { ButtonHTMLAttributes } from 'react'

type Tono = 'sol' | 'hoja' | 'coral' | 'cielo' | 'tinta'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  tono?: Tono
}

export function BotonGrande({ tono = 'sol', className = '', type = 'button', children, ...resto }: Props) {
  return (
    <button type={type} className={`boton-grande tono-${tono} ${className}`.trim()} {...resto}>
      {children}
    </button>
  )
}
