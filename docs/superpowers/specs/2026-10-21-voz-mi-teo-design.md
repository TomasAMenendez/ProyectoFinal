# Voz de TEO en Mi TEO — diseño

Fecha: 2026-10-21  
Producto: app Android, peluche y ServidorDePC  
Estado: aprobado en conversación; pendiente de plan de implementación

## Problema

ServidorDePC ya puede hablar con Topa-e1500 o con Micaela, pero la voz activa está fija en el proceso. Para oír la otra hay que cambiar código y reiniciar el servidor. En Ajustes de TEO no existe una sección para elegirla.

## Decisiones

- La sección se llama **Voz**. Ofrece dos opciones excluyentes: **Voz de Topa** (masculina) y **Voz de Micaela** (femenina).
- Al tocar una opción, la próxima frase de TEO usa esa voz. No hay botón Aplicar.
- La elección se guarda en el peluche, igual que el volumen. Sobrevive a apagar el peluche o el servidor de PC.
- El cambio no reinicia el servidor de PC ni el proceso del peluche.
- Los identificadores son `topa` y `micaela`.
- Si no hay voz guardada, la voz es `topa`.
- Topa usa length 1.45, noise 1.00 y noise W 0.50. Micaela sigue con las escalas de su JSON: 1.0, 0.667 y 0.8.
- La Raspberry Pi, cuando sintetiza en local porque la PC no responde, sigue con Daniela. La elección guardada no cambia ese fallback.

## Flujo

1. La app lee `tts_voice` del estado del peluche y marca la opción correspondiente.
2. Al elegir, envía `POST /api/config` con solo `{"tts_voice": "topa"}` o `{"tts_voice": "micaela"}`.
3. El peluche valida, guarda y responde la voz aceptada.
4. En cada `POST /v1/audio/speech`, el peluche manda `{"input": "...", "language": "es", "voice": "topa"|"micaela"}` con la voz guardada en ese momento.
5. La PC sintetiza esa frase con el perfil pedido. Las dos voces ya están cargadas desde el arranque.

Un ajuste de volumen, brillo o tiempo de juego no incluye `tts_voice` y no la borra.

## App

Tarjeta nueva en Ajustes de TEO, debajo de la tarjeta de volumen, brillo y tiempo de juego, y arriba de la tarjeta de modo noche.

- Título: Voz.
- Primera opción: título «Voz de Topa» y texto secundario «Masculina».
- Segunda opción: título «Voz de Micaela» y texto secundario «Femenina».
- Hasta recibir el estado del peluche, las dos opciones están deshabilitadas.
- Si el estado no trae `tts_voice`, se muestra Topa.
- Al tocar una opción distinta de la confirmada, esa queda marcada y se envía enseguida. Durante el envío, las opciones quedan deshabilitadas. Tocar la voz ya confirmada no envía otro pedido.
- Si el peluche confirma la misma voz, esa queda seleccionada.
- Si el envío falla, no hay respuesta o la voz confirmada no es la enviada, la selección vuelve a la última voz confirmada y el estado de la pantalla muestra el error que ya usa Ajustes.
- La vista previa de depuración usa el mismo layout, con Topa seleccionado como ejemplo. No consulta al peluche, no envía comandos y no guarda datos reales.

La lectura y el cuerpo del pedido viven en reglas puras, aparte de la vista.

## Peluche

`tts_voice` entra en las preferencias que ya persisten volumen, brillo y tiempo de juego, en el estado que ve la app y en la respuesta de `POST /api/config`.

- Valores aceptados: `topa` y `micaela`, en minúsculas.
- Cualquier otro valor, incluido vacío o de otro tipo, rechaza el `POST /api/config` con 400 y deja la voz anterior.
- Un cuerpo sin `tts_voice` no modifica la voz guardada.
- Al cargar preferencias viejas sin el campo, la voz en memoria y en el estado es `topa`.
- El cliente de la PC lee la voz del estado del peluche en cada frase, no una copia tomada al arrancar.

## Servidor de PC

Al arrancar carga Topa y Micaela. `tts` en `/health` es verdadero si al menos una de las dos cargó.

`POST /v1/audio/speech`:

| `voice` | Resultado |
|---|---|
| `topa` | Piper con el perfil Topa |
| `micaela` | Piper con el perfil Micaela |
| ausente, vacío o desconocido | Piper con el perfil Topa |
| voz pedida que no llegó a cargar | respuesta vacía o error, sin sustituir por la otra voz |

Elena no interviene en este endpoint. El archivo y la variable `prefer_elena` dejan de elegir el motor de estas frases.

El perfil Micaela permanece en el código con sus archivos y sus escalas actuales, para poder volver a usarlo. La voz por defecto del servidor, cuando el pedido no trae una voz válida, es Topa.

## Errores

- App sin conexión o respuesta inválida: vuelve a la última voz confirmada.
- Peluche no puede guardar: 500, la voz anterior sigue, la app revierte.
- PC caída o frase rechazada: el peluche reproduce con Daniela esa frase. La voz guardada sigue igual y la siguiente frase vuelve a pedirla.

## Pruebas

- Reglas de la app: arman el cuerpo con `topa` o `micaela`, leen el estado, y si falta el campo muestran Topa.
- Peluche: guarda y recupera la voz tras recargar preferencias; un `POST` de volumen no la borra; un valor inválido responde 400 y conserva la anterior; el pedido a la PC incluye la voz del estado.
- PC: Topa sintetiza con 1.45, 1.00 y 0.50; Micaela con 1.0, 0.667 y 0.8; una voz desconocida usa Topa; cambiar de perfil no recarga el proceso; Elena no se llama en `/v1/audio/speech`.

## Fuera de alcance

- Cambiar la voz local Daniela de la Pi.
- Más voces, vista previa de audio en el celular, o un botón Aplicar.
- Reiniciar el servidor de PC como parte del cambio de voz.
