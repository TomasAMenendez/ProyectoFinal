import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const archivo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'offline', 'AppParaExpo-Offline.html')
if (!fs.existsSync(archivo)) {
  console.error('No está el archivo offline.')
  process.exit(1)
}
const html = fs.readFileSync(archivo, 'utf8')
const hallazgos = []
const atributos = html.match(/(?:src|href|url)\(\s*['"]?(https?:\/\/[^"')\s]+)/g) ?? []
const etiquetas = html.match(/(?:src|href)=['"]https?:\/\/[^"']+/g) ?? []
for (const hallazgo of [...atributos, ...etiquetas]) hallazgos.push(hallazgo)
if (html.includes('api.cartesia.ai') || html.includes('fonts.googleapis') || html.includes('fonts.gstatic') || html.includes('proyectoteo.onrender.com')) {
  hallazgos.push('servicio externo')
}
if (hallazgos.length) {
  console.error(`El HTML autónomo pide recursos externos: ${hallazgos.slice(0, 8).join(', ')}`)
  process.exit(1)
}
console.log(`HTML autónomo listo (${Math.round(html.length / 1024)} KB).`)
