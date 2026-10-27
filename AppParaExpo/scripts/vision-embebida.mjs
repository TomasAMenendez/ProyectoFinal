import fs from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'

const ID = 'virtual:vision-embebida'
const RESUELTO = '\0virtual:vision-embebida'

function gzipBase64(archivo) {
  return zlib.gzipSync(fs.readFileSync(archivo)).toString('base64')
}

function moduloHosted() {
  return `export async function prepararVision(cual) {
  const vision = await import('@mediapipe/tasks-vision')
  const base = new URL('reconocimiento/wasm/', window.location.href).toString()
  const nombre = cual === 'cara' ? 'face_landmarker.task' : 'gesture_recognizer.task'
  const fileset = await vision.FilesetResolver.forVisionTasks(base)
  return {
    fileset,
    modelo: { modelAssetPath: new URL('reconocimiento/' + nombre, window.location.href).toString() },
  }
}
`
}

function moduloOffline(raiz) {
  const carpeta = path.join(raiz, 'reconocimiento')
  const piezas = {
    simdJs: gzipBase64(path.join(carpeta, 'wasm', 'vision_wasm_internal.js')),
    simdWasm: gzipBase64(path.join(carpeta, 'wasm', 'vision_wasm_internal.wasm')),
    nosimdJs: gzipBase64(path.join(carpeta, 'wasm', 'vision_wasm_nosimd_internal.js')),
    nosimdWasm: gzipBase64(path.join(carpeta, 'wasm', 'vision_wasm_nosimd_internal.wasm')),
    cara: gzipBase64(path.join(carpeta, 'face_landmarker.task')),
    mano: gzipBase64(path.join(carpeta, 'gesture_recognizer.task')),
  }
  return `const piezas = ${JSON.stringify(piezas)}
const listos = new Map()
const sondaSimd = new Uint8Array([0,97,115,109,1,0,0,0,1,5,1,96,0,1,123,3,2,1,0,10,10,1,8,0,65,0,253,15,253,98,11])

async function bytes(clave) {
  const guardado = listos.get(clave)
  if (guardado) return guardado
  const crudo = Uint8Array.from(atob(piezas[clave]), (letra) => letra.charCodeAt(0))
  const flujo = new Blob([crudo]).stream().pipeThrough(new DecompressionStream('gzip'))
  const plano = new Uint8Array(await new Response(flujo).arrayBuffer())
  listos.set(clave, plano)
  return plano
}

async function usaSimd() {
  try {
    await WebAssembly.instantiate(sondaSimd)
    return true
  } catch {
    return false
  }
}

export async function prepararVision(cual) {
  const simd = await usaSimd()
  const js = await bytes(simd ? 'simdJs' : 'nosimdJs')
  const wasm = await bytes(simd ? 'simdWasm' : 'nosimdWasm')
  const modelo = await bytes(cual === 'cara' ? 'cara' : 'mano')
  return {
    fileset: {
      wasmLoaderPath: URL.createObjectURL(new Blob([js], { type: 'text/javascript' })),
      wasmBinaryPath: URL.createObjectURL(new Blob([wasm], { type: 'application/wasm' })),
    },
    modelo: { modelAssetBuffer: modelo },
  }
}
`
}

export function visionEmbebida(raiz, offline) {
  return {
    name: 'vision-embebida',
    resolveId(source) {
      if (source === ID) return RESUELTO
    },
    load(id) {
      if (id !== RESUELTO) return null
      return offline ? moduloOffline(raiz) : moduloHosted()
    },
  }
}
