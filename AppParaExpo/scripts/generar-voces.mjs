import { spawn } from 'node:child_process'
import { createRequire } from 'node:module'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const ffmpeg = require('ffmpeg-static')
const app = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

if (!ffmpeg) {
  console.error('ffmpeg no está disponible. La generación de voces se detiene.')
  process.exit(1)
}

const proceso = spawn('python', [path.join(app, 'scripts', 'generar-voces.py')], {
  cwd: app,
  stdio: 'inherit',
  env: { ...process.env, FFMPEG: ffmpeg, PYTHONIOENCODING: 'utf-8' },
})

proceso.on('exit', (codigo) => process.exit(codigo ?? 1))
