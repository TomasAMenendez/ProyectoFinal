# Data model: Base visual e Inicio de TEO

Los datos viven en memoria de la app o llegan de contratos ya existentes. Este bloque no crea almacenamiento permanente ni cambia el modelo del peluche.

## Estado visible de TEO

No es una entidad guardada. Se deriva al mostrar Inicio.

| Campo visible | Origen | Regla |
|---|---|---|
| Conexión | `RobotConnectionManager.isConnected` | Conectado o Desconectado. |
| Actividad | `pending_wake`, `models_ready` y `power_on` de `GET /api/status` | Solo si hay conexión vigente. |
| Acción principal | Actividad derivada | Dormir TEO o Despertar TEO. |
| Personaje | `teo-saludando.png`, presentado con el encuadre de su contenido opaco | La vista usa ese cálculo. Las proporciones se conservan y el archivo original no se recorta. `teo.png` no representa sueño ni desconexión. |

Transiciones de actividad, con conexión vigente:

1. `pending_wake` verdadero: Preparándose. No mostrar Despierto.
2. Si no hay despertar pendiente y `models_ready` es falso y TEO no está despierto: Preparándose.
3. `models_ready` verdadero, sin `pending_wake` y sin actividad despierta: Dormido. La acción es Despertar TEO.
4. Actividad despierta, sin `pending_wake`: Despierto. La acción es Dormir TEO.

`power_on` del estado no se lee solo. En el peluche incluye el despertar pendiente.

Sin conexión, la actividad es no disponible. No se reutiliza el último despierto o dormido como si siguiera vigente. La reproducción conocida tampoco se afirma como actual: se indica que dejó de estar vigente y no se anuncia como iniciada ni detenida.

## Resumen de hoy

| Campo | Origen | Validación |
|---|---|---|
| Interacciones | `summary.total_interactions` en la telemetría de hoy | Un número válido, incluido cero, se muestra. Si la consulta falla o el campo falta o es inválido, es ausencia. El valor inicial no es cero. Un cero de respaldo enviado como número no se distingue de un cero registrado. |
| Señal emocional | Último `events[].emotion` no vacío de esa telemetría | Se muestra como último registro, sin emoji. Sin evento utilizable, es ausencia. |

La duración y el límite de uso no se muestran en Inicio. Sus campos actuales pueden seguir existiendo para no cambiar Configuración.

## Reproducción actual

| Campo | Origen | Validación |
|---|---|---|
| Cuento activo | `currently_reading.title` e `id` | Los dos tienen que tener texto. |
| Canción activa | `currently_playing` | Solo si no hay cuento activo. |
| Nada | Ambos vacíos | Detener queda visible y deshabilitado. |
| Catálogo | Lista `songs` de `GET /api/music` | Vacío y sin reproducción: no se inicia audio y se orienta a Contenido. |

Si hay cuento activo, la acción visible es «Volver a leer cuento» y vuelve a solicitar ese identificador. Si no hay cuento, la acción es «Reproducir música»: última canción elegida que siga en el catálogo o, si no está, la primera.

Detener se habilita solo cuando el estado vigente muestra canción o cuento. La app lee el cuerpo de la respuesta y después el estado. Si la solicitud se rechaza, la consulta falla o el estado posterior no confirma la acción, no se anuncia éxito ni se borra lo conocido. Al confirmarse una detención, el nuevo estado deja de mostrar esa reproducción.

## Aviso retenido

| Campo | Obligatorio | Regla |
|---|---|---|
| Tipo | Sí | `aviso`, `logro`, `pedido` o `crisis`, ya normalizado. Otros tipos se siguen rechazando. |
| Mensaje | Sí | Texto no vacío. |
| Fecha del evento | No | Se parsea el `timestamp` recibido. Si no es válido, queda ausente. |
| Momento de recepción | Sí | Se fija una vez, al incorporar el aviso. No se actualiza al abrir una pantalla. |

Etiquetas visibles: Logro, Pedido, Atención para `crisis`, y Aviso. Sin corchetes y sin emoji.

Orden, de más nuevo a más antiguo: fecha del evento si es válida; si no, momento de recepción. Ese orden elige el aviso de Inicio, la lista y cuáles veinte se conservan. Al llegar uno que deja la lista en veintiuno, sale el más antiguo según el mismo orden.

La fecha mostrada es solo la del evento. El momento de recepción no se presenta.

La lista no tiene leído, archivo ni identificador persistente. Se pierde al cerrar el proceso, igual que hoy.

## Resultado de una acción

| Acción | Confirmación | Si no se confirma |
|---|---|---|
| Dormir o despertar | `power_on`, `models_ready` y `pending_wake` de la respuesta, y una consulta posterior de estado | Se conserva el último estado vigente y se muestra el mensaje disponible. |
| Celebrar | Cuerpo que confirma celebración enviada. Se informa «Celebración enviada a TEO» y no que el festejo terminó. | Se explica la condición real. No se anuncia el envío ni un festejo terminado. |
| Reproducir música o volver a leer | El cuerpo y el estado posterior muestran esa canción o ese cuento | Se conserva lo conocido, no se anuncia éxito y se explica el resultado. |
| Detener | El cuerpo y el estado posterior ya no muestran esa reproducción | Se conserva lo conocido, Detener puede seguir habilitado y se explica el problema. |

Mientras una acción está en curso, no se puede repetir. No se muestra un porcentaje. Al terminar en éxito o en error, los controles vuelven según la disponibilidad real de TEO.

## Relaciones

- Inicio y la lista de avisos leen la misma colección retenida. Ninguno pide de nuevo la cola.
- El poll que ya existe sigue siendo el único que llama a `GET /api/notifications`.
- Las notificaciones del sistema siguen usando tipo y mensaje. No dependen del orden ni de la fecha visible.
- Actividad, Contenido, Rutinas, Agenda, Configuración y el primer uso no reciben este modelo.
