export interface ConfigRegistro {
  habilitado: boolean
  url: string
  apiKey: string
}

export function configDeCompilacion(): ConfigRegistro {
  return {
    habilitado: true,
    url: import.meta.env.VITE_SHEETS_URL ?? '',
    apiKey: import.meta.env.VITE_SHEETS_API_KEY ?? '',
  }
}
