# Hallazgo automático ServidorDePC ↔ Raspberry Pi — diseño

Fecha: 2026-10-05  
Producto: `ServidorDePC/` + `ProyectoParaRasperrypiV5`  
Estado: diseño aprobado en conversación

## Problema

La Pi solo habla con la PC si `PC_SERVER_URL` o `pc_server_url.txt` ya tienen la dirección, y el token está en el entorno o en `pc_server_token.txt`. El DHCP del hotspot cambia esa IP al recrear la red. Hay que cargarla a mano.

La app Android no carga la IP del peluche a mano. La Pi anuncia por UDP (puerto 5555, cada 1,5 s) un JSON con IP, puerto y token de vínculo. El celular escucha. Si el broadcast no llega, recorre la subred por HTTP.

En este hotspot el unicast PC ↔ Pi funciona. El broadcast a veces no cruza. El servidor de la PC escucha en `0.0.0.0:8090` a propósito: esa dirección no es la IP de la LAN.

## Objetivo

Con la PC y la Pi en la misma LAN, se encuentran solas. El anuncio lleva IP, puerto y token. Quien esté en esa Wi‑Fi puede usar el servidor. No hay archivos que copiar.

Si el broadcast no llega, la Pi igual encuentra la PC por HTTP. Si la PC no está, el peluche sigue con STT y LLM locales. El turno de voz no espera el barrido.

## Fuera de alcance

- mDNS / DNS-SD.
- Elegir entre varias PC. Gana la primera que responda.
- Código de confirmación en la consola.
- Cambiar el beacon del peluche (UDP 5555) ni la app Android.
- Poner el token en `GET /health`.
- Buscar la PC dentro del turno de voz.

## Arquitectura

La PC anuncia. La Pi escucha y, si no oye nada, barre la subred. El turno de voz no espera ese barrido: sin servidor conocido, sigue Whisper y el modelo local.

`ServidorDePC` manda cada 1,5 s un UDP a `<broadcast>` en el puerto **5556**, con `SO_BROADCAST`. El 5555 queda para el peluche y el celular. El mismo JSON lo sirve `GET /discover`, sin autenticación. `GET /health` sigue exigiendo `Authorization: Bearer`.

`/discover` responde aunque Whisper y Gemma todavía no estén listos. El beacon UDP arranca al inicio de `main()`, antes de `load()`. `/discover` existe cuando Uvicorn ya escucha, después de esa carga. La Pi reintenta, así que el barrido alcanza a la PC en cuanto el HTTP está arriba.

La Pi no usa una IP escrita en el JSON. En el beacon usa la dirección de origen del paquete UDP. En el barrido usa la IP que respondió. Sirve aunque Windows no tenga ruta a Internet.

IPv4 solamente.

## JSON

```json
{
  "device_id": "teo_pc_server",
  "device_name": "TEO ServidorDePC",
  "api_port": 8090,
  "token": "<pc_server_token>"
}
```

`api_port` es el `PORT` real del servidor (default 8090). El token es el de `load_or_create_token()`, el mismo de `pc_server_token.txt` en la PC. El log muestra el puerto y los primeros 8 caracteres del token, no el token entero.

## Componentes

**`ServidorDePC/discovery.py`** arma el JSON y envía el UDP. No conoce FastAPI.

**`ServidorDePC/server.py`** arranca el hilo del beacon al entrar a `main()` y publica `GET /discover` con ese JSON, sin token.

**`ProyectoParaRasperrypiV5/pc_discovery.py`** es un hilo daemon. Lo arrancan los dos caminos de `app.py` que hoy llaman `PcServerClient.from_env()`, sobre esa misma instancia. `AudioWorker` y `SpeechWorker` ya comparten el cliente: alcanza con actualizarlo.

**`pc_server_client.py`** no cambia de contrato HTTP. El hallazgo escribe `base_url` y `token` en el cliente vivo y en `pc_server_url.txt` y `pc_server_token.txt`.

Si `PC_SERVER_URL` y `PC_SERVER_TOKEN` están las dos en el entorno, el hilo no arranca. Los tests no barrean la red. Si solo está una de las dos, el hilo tampoco arranca: queda el comportamiento de hoy.

## Flujo en la Pi

Sin variables de entorno, el hilo repite esto:

1. Si ya hay URL y token (memoria o caché), hace `GET /health` con el timeout ya existente (0,3 s). **200** o **503**: se queda con esa PC y pone en cero el contador de timeouts. **503** significa "cargando", no "perdida". La sonda siguiente es a los 15 s.
2. Busca de nuevo cuando no hay URL, el estado es **401**, la conexión es rechazada, no hay ruta o el host es inalcanzable, o la sonda acumuló dos timeouts seguidos. Escucha UDP 5556 durante 3 s de reloj. Acepta solo `device_id` `teo_pc_server`, token no vacío y una IP de origen usable (no vacía, no `0.0.0.0`, no loopback, no broadcast). El puerto es `api_port` si es un entero entre 1 y 65535; si no, descarta el paquete.
3. Si no llegó un paquete válido, barre la `/24` de `current_lan_ip()`. 16 hilos, timeout de conexión 0,3 s, saltea la IP de la propia Pi. `GET /discover`. Acepta solo el `device_id` y un token no vacío. La URL es `http://<ip-sondeada>:<api_port>`.
4. Al encontrar: con el lock HTTP del cliente, asigna `base_url` y `token` juntos, pone en cero `fail_count`, `open_until`, `fail_count_tts`, `open_until_tts` y el contador de timeouts, y reescribe los dos `.txt`. Espera 15 s y vuelve al paso 1.
5. Si no encontró: espera 15 s y vuelve al paso 2. Un solo barrido a la vez.

La sonda es de este hilo. No mira el circuit del audio. Un STT lento no dispara el barrido.

Un **401**, una conexión rechazada, sin ruta o inalcanzable, pasan al paso 2 en ese momento, sin esperar los 15 s. Un solo timeout de la sonda no busca: suma uno y espera 15 s para sondear otra vez. Al segundo timeout seguido, pasa al paso 2.

Si no puede escribir los `.txt`, el cliente en memoria igual queda apuntando a la PC. El próximo arranque busca de nuevo.

Si `current_lan_ip()` no devuelve IPv4 usable, no barre. Solo escucha UDP, espera 15 s y repite.

El primer anuncio o la primera respuesta HTTP válidos ganan.

## Errores

El hilo de la Pi atrapa cualquier excepción, la anota y sigue. El audio y Uvicorn no se caen.

| Qué pasa | Qué hace |
|---|---|
| Paquete UDP mal formado, `device_id` ajeno, token vacío, `api_port` inválido o IP de origen inutilizable | Lo ignora y sigue dentro de la ventana de 3 s |
| El socket UDP de la Pi no abre | Pasa al barrido y lo reintenta en el ciclo siguiente |
| Una IP del barrido no contesta, cierra o devuelve otro cuerpo | Sigue con las demás |
| El beacon de la PC no puede enviar | Cierra el socket, lo reabre y sigue. El HTTP no se entera |

El firewall de Windows tiene que dejar entrar TCP 8090 en redes privadas, igual que hoy. El UDP 5556 sale de la PC; el valor por defecto de Windows deja salir ese tráfico.

## Pruebas

Sin red real y sin GPU. Socket UDP y HTTP inyectados.

`ServidorDePC/test_discovery.py`

- El JSON lleva `teo_pc_server`, el puerto configurado y el token.
- `GET /discover` responde 200 sin `Authorization`, con los modelos en frío.
- `GET /health` sin token sigue en 401.
- Si el envío UDP falla, el ciclo reabre el socket y no propaga la excepción.

`ProyectoParaRasperrypiV5/test_pc_discovery.py`

- Un paquete válido usa la IP de origen, no un campo del JSON.
- Se descartan `device_id` ajeno, token vacío, `api_port` inválido e IP inutilizable.
- Si la ventana UDP no oye nada, corre el barrido. El barrido acepta solo `/discover` con ese `device_id` y arma la URL con la IP sondeada. No toca la IP de la Pi.
- Con las dos variables de entorno, no busca ni pisa el cliente. Con una sola, tampoco.
- **200** y **503** dejan la URL. **401** y conexión rechazada buscan de nuevo. Un timeout no; dos seguidos, sí.
- Al adoptar una PC se actualizan URL y token juntos y se cierra el circuit.
- Si falla la escritura de los `.txt`, el cliente en memoria igual apunta a la PC.
- Sin URL, el turno de voz sigue el camino local y no llama al barrido. Los tests de routing existentes siguen pasando.

A mano, en el hotspot: PC arriba, Pi sin `PC_SERVER_URL`. El log de la Pi muestra `http://<ip>:8090`. `curl` a `/health` con el token del anuncio responde. Con el UDP bloqueado, el barrido igual la encuentra. Con la PC apagada, el peluche sigue en local.

## Criterio de hecho

- Misma LAN, sin variables de entorno: la Pi usa la PC para STT/LLM/TTS en el turno siguiente al hallazgo.
- Hotspot que no reenvía broadcast: el barrido HTTP igual deja URL y token.
- PC apagada o fuera de la red: el peluche no se bloquea y usa el camino local.
- `PC_SERVER_URL` + `PC_SERVER_TOKEN` siguen ganando y no disparan barrido.
