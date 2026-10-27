import audioCaperucita from '../../../Cuentos/Caperucita Roja.mp3'
import audioPatito from '../../../Cuentos/El patito feo.mp3'
import audioHansel from '../../../Cuentos/Hansel y Gretel.mp3'
import audioChanchitos from '../../../Cuentos/Los 3 chanchitos.mp3'
import audioRicitos from '../../../Cuentos/Ricitos de Oro.mp3'
import textoCaperucita from '../../../Cuentos/Caperucita Roja.txt?raw'
import textoPatito from '../../../Cuentos/El patito feo.txt?raw'
import textoHansel from '../../../Cuentos/Hansel y Gretel.txt?raw'
import textoChanchitos from '../../../Cuentos/Los 3 chanchitos.txt?raw'
import textoRicitos from '../../../Cuentos/Ricitos de Oro.txt?raw'

export interface Cuento {
  id: string
  titulo: string
  imagen: string
  audio: string
  texto: string
}

export const CUENTOS: Cuento[] = [
  { id: 'caperucita', titulo: 'Caperucita Roja', imagen: 'Imagen_Casa.png', audio: audioCaperucita, texto: textoCaperucita },
  { id: 'chanchitos', titulo: 'Los 3 chanchitos', imagen: 'Imagen_Chancho.png', audio: audioChanchitos, texto: textoChanchitos },
  { id: 'ricitos', titulo: 'Ricitos de Oro', imagen: 'Imagen_Oso.png', audio: audioRicitos, texto: textoRicitos },
  { id: 'patito', titulo: 'El patito feo', imagen: 'Imagen_Pato.png', audio: audioPatito, texto: textoPatito },
  { id: 'hansel', titulo: 'Hansel y Gretel', imagen: 'Imagen_Torta.png', audio: audioHansel, texto: textoHansel },
]
