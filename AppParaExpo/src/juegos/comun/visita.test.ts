import { describe, expect, it } from 'vitest'
import { abrirJuego, confirmarNombre, nombreValido, visitaInicial, volverAlMenu } from './visita'

describe('visita', () => {
  it('rechaza un nombre vacio y recorta espacios', () => {
    expect(nombreValido('   ')).toBeNull()
    expect(nombreValido('  Ana   Luz  ')).toBe('Ana Luz')
    const larga = nombreValido('a'.repeat(50))
    expect(larga?.length).toBe(40)
    expect(confirmarNombre(visitaInicial(), '   ').pantalla).toBe('nombre')
  })

  it('conserva el nombre al entrar a un juego y al volver', () => {
    const menu = confirmarNombre(visitaInicial(), 'Teo')
    const juego = abrirJuego(menu, 'sombras')
    expect(juego).toEqual({ nombre: 'Teo', pantalla: 'sombras' })
    expect(volverAlMenu(juego)).toEqual({ nombre: 'Teo', pantalla: 'menu' })
  })
})
