import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'
import { viteSingleFile } from 'vite-plugin-singlefile'
import { visionEmbebida } from './scripts/vision-embebida.mjs'

const raiz = path.dirname(fileURLToPath(import.meta.url))

function tipoReconocimiento(archivo: string): string {
  if (archivo.endsWith('.wasm')) return 'application/wasm'
  if (archivo.endsWith('.js')) return 'text/javascript'
  return 'application/octet-stream'
}

export default defineConfig(({ mode }) => {
  const offline = mode === 'offline'
  const android = mode === 'android'
  const sinRed = offline || android
  const salida = android
    ? path.join(raiz, '..', 'AppParaExpoAndoid', 'app', 'src', 'main', 'assets', 'www')
    : path.join(raiz, offline ? 'offline' : 'dist')
  return {
    base: './',
    plugins: [
      react(),
      visionEmbebida(raiz, offline),
      ...(offline ? [viteSingleFile()] : []),
      {
        name: 'renombrar-html-offline',
        apply: 'build',
        closeBundle() {
          if (!offline) return
          const origen = path.join(raiz, 'offline', 'index.html')
          const destino = path.join(raiz, 'offline', 'AppParaExpo-Offline.html')
          if (!fs.existsSync(origen)) return
          fs.rmSync(destino, { force: true })
          fs.renameSync(origen, destino)
        },
      },
      {
        name: 'reconocimiento-local',
        configureServer(server) {
          const carpeta = path.resolve(raiz, 'reconocimiento')
          server.middlewares.use('/reconocimiento', (req, res, next) => {
            const pedido = decodeURIComponent(((req as { url?: string }).url ?? '/').split('?')[0]).replace(/^\/+/, '')
            const archivo = path.resolve(carpeta, pedido)
            const relativo = path.relative(carpeta, archivo)
            if (relativo.startsWith('..') || path.isAbsolute(relativo) || !fs.existsSync(archivo) || fs.statSync(archivo).isDirectory()) {
              next()
              return
            }
            res.setHeader('Content-Type', tipoReconocimiento(archivo))
            fs.createReadStream(archivo).pipe(res)
          })
        },
        closeBundle() {
          const origen = path.join(raiz, 'reconocimiento')
          const destino = path.join(salida, 'reconocimiento')
          if (!fs.existsSync(origen)) return
          fs.cpSync(origen, destino, { recursive: true })
        },
      },
    ],
    resolve: {
      alias: {
        '@registro-config': path.join(
          raiz,
          'src',
          'servicios',
          sinRed ? 'config.offline.ts' : 'config.hosted.ts',
        ),
        '@enlace-vestir': path.join(raiz, 'src', 'pantallas', sinRed ? 'vestir.offline.ts' : 'vestir.hosted.ts'),
        '@cuentos': path.join(raiz, 'src', 'pantallas', sinRed ? 'cuentos.offline.ts' : 'cuentos.hosted.ts'),
      },
    },
    define: {
      __REGISTRO_HABILITADO__: sinRed ? 'false' : 'true',
      __ENTREGA_OFFLINE__: offline ? 'true' : 'false',
    },
    build: {
      outDir: salida,
      emptyOutDir: true,
      assetsInlineLimit: offline ? 100_000_000 : 4096,
      cssCodeSplit: false,
    },
    test: {
      environment: 'node',
      include: ['src/**/*.test.ts'],
    },
  }
})
