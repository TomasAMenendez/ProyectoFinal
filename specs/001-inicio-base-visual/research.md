# Research: Base visual e Inicio de TEO

## Tema claro sin recolorear las pantallas beta

- **Decision**: Agregar colores y estilos `teo_` y aplicarlos solo mientras el destino visible sea Inicio o la lista nueva de avisos. El tema de la aplicación sigue siendo `Theme.AplicacionParaCelular`. La lista entra en ese tema en el mismo corte que crea su destino. Hasta entonces no se cita ese destino. Al entrar en una pantalla beta se restaura la apariencia beta. Los botones `teo_` conservan las mayúsculas escritas.
- **Rationale**: La constitución exige Inicio claro aunque el teléfono esté en modo oscuro, y prohíbe cambiar el tema global de las pantallas beta. Esas pantallas usan hoy la paleta lavanda y `values-night`. La especificación pide lo mismo para la lista nueva. Material, si no se lo impide, pasa los botones a mayúsculas.
- **Alternatives considered**: Reemplazar `colorPrimary` en el tema de la app. Se rechaza porque recolorearía Rutinas, Archivos, Agenda y Configuración. Forzar el modo claro de todo el proceso también se rechaza por el mismo motivo.

## Personaje, logo y confeti

- **Decision**: Copiar sin recortar `docs/ui-ux/referencias/logo-teo.png`, `teo-saludando.png` y `teo.png`. El encuadre de Inicio usa el rectángulo de píxeles no transparentes de la pose saludando, con escala uniforme, para acercar su tamaño visible al del boceto. La vista de Inicio presenta `teo-saludando.png` con ese cálculo. `fitCenter` no es el encuadre aceptado. La pose de frente queda guardada y no representa sueño. Actualización del 14 de octubre: se usa el logo PNG transparente actualizado y `fondo-logros.jpg`, entregados por el usuario; el fondo sustituye al confeti vectorial de preparación y permanece separado del texto nativo. Una imagen decorativa no determina la altura de su tarjeta.
- **Rationale**: `fitCenter` escala el archivo completo. En `teo-saludando.png` el lienzo mide 1365×768 y el contenido visible va de 371,34 a 927,738, así que el oso ocuparía solo una parte del hueco. Los archivos originales deben conservarse. Las proporciones del contenido visible no se estiran. Una prueba del cálculo no presenta el personaje si la vista no lo usa.
- **Alternatives considered**: Recortar los PNG de `docs/ui-ux/referencias` o fabricar otro personaje. Quedó descartado. Usar solo `fitCenter` también quedó descartado: mantiene los márgenes transparentes y achica el oso respecto del boceto.

## Estados de conexión y actividad

- **Decision**: La conexión sale de `RobotConnectionManager.isConnected`. La actividad, solo con conexión vigente, sale de `GET /api/status`, leyendo `pending_wake` antes que `power_on`.
- **Rationale**: En el peluche, `status_power_on()` es verdadero si está despierto o si hay un despertar pendiente. Usar solo `power_on` mostraría Despierto durante Preparándose. `phase()` distingue despierto, dormido y carga de modelos.
- **Alternatives considered**: Confirmar el despertar con el valor pedido por la app. `DashboardViewModel.togglePower` hoy hace eso ante cualquier respuesta exitosa. Se rechaza porque una solicitud aceptada no prueba que TEO ya esté despierto.

## Confirmación de celebración, música y detención

- **Decision**: Celebrar informa «Celebración enviada a TEO» solo cuando el cuerpo trae el resultado de celebración enviada. No existe un estado de festejo terminado y la interfaz no lo inventa. Dormido, desconectado o no disponible se explican como condición. Música y detención leen el cuerpo y se confirman con el estado posterior, no solo con HTTP exitoso. Si la solicitud se rechaza, la consulta falla o el estado no confirma, no se anuncia éxito ni se borra lo conocido. Si se pierde la conexión, esa reproducción deja de presentarse como vigente.
- **Rationale**: `POST /api/celebrate` puede responder que la celebración fue enviada, que TEO está dormido dentro de una respuesta exitosa, o que no está disponible. Ninguna de esas respuestas dice que el festejo terminó.
- **Alternatives considered**: Exigir una confirmación de festejo finalizado. Se rechaza porque el contrato actual no la proporciona y este bloque no agrega APIs. Tratar todo HTTP exitoso como éxito también se rechaza.

## Cuento activo

- **Decision**: Si `currently_reading` trae título e identificador, Ahora con TEO muestra ese título y la acción principal dice «Volver a leer cuento». Vuelve a solicitar ese cuento. No promete continuar desde un punto guardado.
- **Rationale**: La app ya vuelve a llamar a la lectura del cuento activo. La etiqueta «Reproducir música» sería falsa en ese caso. Sin cuento activo, se conserva «Reproducir música» y el criterio de la última canción disponible o la primera del catálogo.
- **Alternatives considered**: Dejar la etiqueta de música también con un cuento activo. Se rechaza porque la especificación pide que la etiqueta coincida con la acción.

## Interacciones y señal emocional

- **Decision**: El globo muestra `summary.total_interactions` cuando el valor es un número válido, incluido cero. Si la consulta falla o el campo falta o es inválido, muestra ausencia. El valor inicial no es cero. La señal sale del último evento de telemetría del día que tenga emoción. Si no hay, muestra ausencia, sin emoji.
- **Rationale**: Si falta el archivo de hoy, el servidor puede responder con la telemetría en memoria. Si no hay subsistema de telemetría, o si el día pedido no es hoy y no hay archivo, puede enviar ceros de respaldo. La app no puede separar todos esos ceros de un cero registrado. Este bloque no cambia ese contrato.
- **Alternatives considered**: Usar `current_emotion` del estado como último registro. Se rechaza. Prometer en este bloque una distinción completa entre cero de respaldo y cero registrado también se rechaza, porque exigiría ampliar la API.

## Avisos

- **Decision**: Guardar tipo, mensaje, timestamp del evento ya parseado y un momento de recepción fijado una sola vez. Ordenar del más nuevo al más antiguo por la fecha del evento si es válida y, si no, por la recepción. Conservar veinte según ese orden. El cambio de `parentAlerts` y la actualización de su único observador, `DashboardFragment`, ocurren en el mismo corte compilable. La presentación del aviso destacado es posterior. Inicio y la lista observan esa lista. No se agrega otro pedido a `GET /api/notifications`.
- **Rationale**: Hoy la app guarda solo una línea con emoji y descarta el timestamp. El único consumidor de la lista es Inicio. El poll existente ya vacía la cola del peluche. Las notificaciones del sistema usan tipo y mensaje por separado y pueden seguir haciéndolo.
- **Alternatives considered**: Dejar los avisos sin fecha siempre al final. Quedó reemplazado por la aclaración del 14 de octubre de 2026. Pedir la cola otra vez al abrir la lista se rechaza porque la vaciaría.

## Insets y barra inferior

- **Decision**: El encabezado de Inicio, su desplazamiento y la lista de avisos respetan los insets reales. La barra inferior compartida recibe el inset inferior para no quedar bajo la navegación del sistema. Ese margen se aplica en la actividad y en los dos fragments, además del XML, y no se suma dos veces. El texto ampliado conserva las etiquetas y el desplazamiento. No se copian alturas fijas de una captura ni se recolorea la barra fuera de Inicio y avisos.
- **Rationale**: El proyecto apunta a SDK 36 y el dispositivo de prueba es un Galaxy S25, con gestos o botones todavía no registrados. `fitsSystemWindows` ya está en el cajón, pero no reemplaza una comprobación en el teléfono.
- **Alternatives considered**: Reservar 24 dp o 48 dp fijos para las barras. Se rechaza porque el modelo no determina el espacio útil.

## Comprobación

- **Decision**: La lógica de estado, orden de avisos, confirmación de reproducción, celebración, acción en curso y ausencia de datos se prueba en unidad, sin teléfono. La compilación no cuenta como fidelidad visual ni como prueba con el peluche. La comparación de composición queda para una captura del S25 y una ventana de 360 dp con texto ampliado.
- **Rationale**: La constitución separa compilar de dar el bloque por terminado. No se inventan porcentajes de parecido ni tiempos máximos de respuesta.
- **Alternatives considered**: Agregar pruebas de instrumento que solo comparen colores XML. Se rechaza: no demuestran el comportamiento ni la captura.
