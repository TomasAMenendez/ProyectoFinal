import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const app = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const repo = path.resolve(app, '..')
const juegos = path.join(repo, 'ProyectoParaRasperrypiV5', 'juegos')
const media = path.join(juegos, 'media')
const datos = path.join(app, 'src', 'datos')
const imagenes = path.join(app, 'src', 'recursos', 'imagenes')

fs.mkdirSync(datos, { recursive: true })
fs.mkdirSync(imagenes, { recursive: true })

function listarMedios() {
  return fs.readdirSync(media)
}

function buscarMedio(nombre) {
  const directo = path.join(media, nombre)
  if (fs.existsSync(directo)) return directo
  const buscado = nombre.normalize('NFC').toLowerCase()
  const hallado = listarMedios().find((archivo) => archivo.normalize('NFC').toLowerCase() === buscado)
  return hallado ? path.join(media, hallado) : null
}

function copiarImagen(nombre, destinoNombre = nombre) {
  const origen = buscarMedio(nombre)
  if (!origen) {
    console.warn(`Falta la imagen: ${nombre}`)
    return false
  }
  fs.copyFileSync(origen, path.join(imagenes, destinoNombre))
  return true
}

const sombras = JSON.parse(fs.readFileSync(path.join(juegos, 'sombras.json'), 'utf8'))
const adivina = JSON.parse(fs.readFileSync(path.join(juegos, 'adivina.json'), 'utf8'))
const contar = JSON.parse(fs.readFileSync(path.join(juegos, 'contar.json'), 'utf8'))
const faltantes = []

sombras.juego_siluetas = sombras.juego_siluetas.filter((ficha) => {
  const destino = ficha.id === 'delfin' ? 'Silueta_Delfin.png' : ficha.silueta_archivo
  const origenNombre = ficha.id === 'delfin' ? 'Silueta_Delfín.png' : ficha.silueta_archivo
  const copiada = copiarImagen(origenNombre, destino) || copiarImagen(ficha.silueta_archivo, destino)
  if (!copiada) {
    faltantes.push(ficha.silueta_archivo)
    return false
  }
  ficha.silueta_archivo = destino
  return true
})

function filtrarPorImagen(lista) {
  return lista.filter((ficha) => {
    if (copiarImagen(ficha.imagen_archivo)) return true
    faltantes.push(ficha.imagen_archivo)
    return false
  })
}

const adivinaLista = filtrarPorImagen(adivina)
const contarLista = filtrarPorImagen(contar)
const logoOrigen = path.join(repo, 'docs', 'ui-ux', 'referencias', 'logo-teo.png')
fs.copyFileSync(logoOrigen, path.join(imagenes, 'logo-teo.png'))

fs.writeFileSync(path.join(datos, 'sombras.json'), JSON.stringify(sombras, null, 2), 'utf8')
fs.writeFileSync(path.join(datos, 'adivina.json'), JSON.stringify(adivinaLista, null, 2), 'utf8')
fs.writeFileSync(path.join(datos, 'contar.json'), JSON.stringify(contarLista, null, 2), 'utf8')

console.log(
  `Recursos copiados: ${sombras.juego_siluetas.length} sombras, ${adivinaLista.length} adivinanzas, ${contarLista.length} conteos.`,
)
if (faltantes.length) {
  console.warn(`Sin imagen y fuera del sorteo: ${faltantes.join(', ')}`)
}
