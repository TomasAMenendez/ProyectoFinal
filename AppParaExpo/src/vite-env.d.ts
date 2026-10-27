/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SHEETS_URL?: string
  readonly VITE_SHEETS_API_KEY?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare const __REGISTRO_HABILITADO__: boolean
declare const __ENTREGA_OFFLINE__: boolean

declare module 'virtual:vision-embebida' {
  export function prepararVision(cual: 'cara' | 'mano'): Promise<{
    fileset: { wasmLoaderPath: string; wasmBinaryPath: string }
    modelo: { modelAssetPath?: string; modelAssetBuffer?: Uint8Array }
  }>
}
