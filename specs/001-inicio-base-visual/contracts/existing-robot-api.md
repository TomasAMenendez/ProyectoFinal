# Contrato existente reutilizado

Este bloque no agrega rutas ni cambia el peluche. La app sigue consumiendo las operaciones que ya tiene.

## Estado

`GET /api/status`

Campos que Inicio usa:

- `power_on`: en el servidor es verdadero también durante `pending_wake`. No alcanza para mostrar Despierto.
- `models_ready`
- `pending_wake`
- `currently_playing`
- `currently_reading`, con `title` e `id` cuando hay un cuento

Inicio no muestra `night_mode`, `belly_wake_enabled`, volumen, brillo ni los campos de tiempo de uso.

## Telemetría de hoy

`GET /api/telemetry/today`

Inicio usa `summary.total_interactions` y el último `events[].emotion` utilizable. No usa el resto del resumen.

Si falta el archivo del día y la fecha pedida es hoy, el servidor puede responder con el resumen y los eventos que tiene en memoria. Si no hay telemetría cargada, o si se pide otro día sin archivo, puede responder con ceros de respaldo. La app muestra un conteo numérico válido, incluido cero. Muestra ausencia solo ante fallo o campo ausente o inválido. El contrato no permite distinguir todos los ceros de respaldo de un cero registrado. Este bloque no agrega un campo ni una ruta para resolverlo.

## Avisos

`GET /api/notifications`

Lo sigue llamando solo el poll ya existente. La respuesta trae `notifications[]` con `type`, `message` y `timestamp`, y vacía la cola del peluche. La lista de Inicio no llama esta ruta.

Tipos aceptados, sin cambio: `aviso`, `logro`, `pedido` y `crisis`.

## Acciones

| Acción de Inicio | Operación existente | Lectura del resultado |
|---|---|---|
| Dormir o despertar | `POST /api/power` | Usar `power_on`, `models_ready`, `pending_wake` y `wake_result`. No dar por despierto un `queued`. |
| Celebrar | `POST /api/celebrate` | El cuerpo puede confirmar que la celebración fue enviada, que TEO está dormido o que no está disponible. No trae un estado de festejo terminado. Solo el primer caso muestra «Celebración enviada a TEO». |
| Reproducir música | `POST /api/music/play` | Confirmar con el estado posterior. |
| Volver a leer cuento | `POST /api/stories/play` | Confirmar con `currently_reading`. No implica un punto guardado. |
| Detener | `POST /api/music/stop` y `POST /api/stories/stop` | Confirmar cuando el estado ya no muestra esa reproducción. |
| Catálogo para iniciar música | `GET /api/music` | `songs` y la última canción elegida que siga presente. |

Estas rutas ya pueden responder que TEO está dormido dentro de una respuesta exitosa. La interfaz tiene que leer ese cuerpo.
