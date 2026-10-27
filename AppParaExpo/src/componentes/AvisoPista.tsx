import { AVISO_PISTA } from '../audio/frases'

export function AvisoPista({ pista }: { pista: string }) {
  return (
    <div className="aviso-pista" role="status">
      <strong>{AVISO_PISTA}</strong>
      <p>{pista}</p>
    </div>
  )
}
