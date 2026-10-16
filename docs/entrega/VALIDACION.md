# Validación de entrega · 15 de octubre de 2026

## Alcance de la evidencia

Se comprobó código Android y Python, comunicación HTTP de la app real desde el Samsung Galaxy S25 y persistencia en almacenamiento aislado. El peluche está en otra casa y no hay acceso remoto; no se desplegó ni se comprobó hardware físico.

## Resultados locales

| Comprobación | Resultado |
| --- | --- |
| `testDebugUnitTest` | 213 pruebas, cero fallos. |
| `testPreviewUnitTest` | 215 pruebas, cero fallos; incluye las pruebas comunes y dos de la vista previa, no son 215 pruebas adicionales distintas. |
| `assembleDebug`, `assemblePreview`, `assembleDebugAndroidTest`, `assembleRelease` | Correctos. Release queda sin firma de publicación. |
| `lintDebug` | Cero errores; siguen advertencias del proyecto, entre ellas APIs obsoletas. |
| Python, once módulos de contratos y regresiones | 203 pruebas, cero fallos. |
| Contratos y persistencia instrumentados en S25 | 17 pruebas, cero fallos; ejecución separada de la prueba de Activity para aislar el singleton de red. |
| Apertura y recreación de MainActivity | Una prueba adicional aprobada con el S25 desbloqueado. Arranque frío manual correcto y servicio de conexión observado en primer plano. Total: 18 pruebas instrumentadas aprobadas. |
| ZIP Android extraído en otra carpeta | `assembleDebug` y las 213 pruebas unitarias pasaron desde la copia extraída, sin `local.properties` ni archivos de la IDE. Se proporcionó el SDK local mediante `ANDROID_HOME`; las dependencias ya estaban descargadas. |

La app real instalada conserva su identificador `com.example.aplicacionparacelular`. Debug y release contienen solamente el lanzador MainActivity. Preview tiene el identificador `com.example.aplicacionparacelular.preview` y su lanzador separado. La captura `docs/ui-ux/validacion/s25-app-real-sin-teo-2026-10-15.png` muestra la búsqueda real, sin datos de ejemplo.

## Integración Android y Python

El S25 utilizó `RobotApiClient` y el `ApiRequestHandler` del producto mediante un puente USB a un servidor local. La validación, biblioteca de cuentos, scheduler y persistencia fueron reales, en carpetas temporales. Sólo se simularon los callbacks físicos; no se ejecutaron audio, cámara, GPIO, BLE ni LCD.

Las ocho pruebas de API cubrieron:

- estado y autorización por token;
- guardado y lectura de los nueve disfraces;
- rutinas con pictograma conocido y sin pictograma, incluida recarga;
- persistencia de volumen/brillo sin sobrescribir otras preferencias;
- subida, listado, reproducción solicitada y borrado de WAV con acentos y caracteres reservados;
- rechazo de reproducción/festejo mientras está dormido y recuperación al despertar;
- avisos con timestamp válido y consumo único de su cola;
- extracción real de un PDF, publicación, edición, lectura solicitada y borrado.

Las demás pruebas de dispositivo comprobaron Agenda, perfil tras restauración, identidad de alarmas y conservación de colecciones corruptas. Usan preferencias aisladas y no borran datos del usuario. La configuración real del cliente HTTP se restaura después de cada prueba de API.

## Correcciones incorporadas

- La Agenda carga los registros guardados antes de agregar uno nuevo y conserva datos malformados para no reemplazarlos silenciosamente.
- Las rutinas requieren una carga válida antes de editar, serializan las escrituras y conservan el último estado confirmado ante un error.
- El scheduler reinicia el estado diario antes de comprobar si la rutina estaba completada.
- El cuento publica su sesión antes del primer bloque de audio, permitiendo solicitar su detención desde el inicio.
- Los nombres de archivos conservan espacios, acentos y caracteres reservados en subida y borrado.
- Volumen y brillo se guardan con reemplazo atómico y se restauran al iniciar. Un fallo de guardado se informa como error.
- La recuperación Wi-Fi tiene límite de espera y reintento LAN; respuestas de una dirección anterior no se adoptan como estado actual.
- El servicio usa el tipo Android `connectedDevice`; UI y servicio comparten la propiedad del polling sin detenerse entre sí.
- Las alarmas de rutinas tienen identidad por id y token por programación: un evento eliminado o antiguo no muestra ni rearma un recordatorio vigente distinto.
- El perfil conserva el modelo y disfraz al recrearse, sin convertir una carga en una edición del usuario.
- MainActivity conserva el NavController del NavHostFragment y evita buscarlo en la vista antes de que se haya adjuntado.

## Pendiente con el peluche

| Prueba | Estado |
| --- | --- |
| Actualizar el código en la Raspberry y reiniciar su entorno existente | Pendiente; se entrega el paquete, no se desplegó. |
| Descubrimiento LAN y BLE/PIN/red Wi-Fi con TEO | Pendiente físico. |
| Dormir, despertar pendiente y celebración ejecutada | Pendiente físico. |
| Audio de música y cuentos; detención efectiva | Pendiente físico. |
| Recordatorio y pictograma ocho segundos, seguido del regreso de los ojos | Pendiente físico. |
| Brillo/volumen efectivos y conservados tras reinicio | Pendiente físico. |
| Avisos originados en TEO recibidos en primer y segundo plano | Pendiente físico. |

Un test HTTP con callbacks simulados no convierte estas filas en aprobadas. El recorrido y la preparación están en `LEEME.md`.
