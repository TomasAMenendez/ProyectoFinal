import type { ConfigRegistro } from './config.hosted'

export function configDeCompilacion(): ConfigRegistro {
  return { habilitado: false, url: '', apiKey: '' }
}
