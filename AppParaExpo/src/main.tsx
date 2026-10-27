import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Aplicacion } from './pantallas/Aplicacion'
import './estilos/base.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Aplicacion />
  </StrictMode>,
)
