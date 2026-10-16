# Propuestas de Contenido

Fecha: 13 de octubre de 2026. Etapa: bocetos, sin implementación.

## Música

Se recibió [esta propuesta de Música](musica-propuesta-2026-10-13.png). Se conserva una copia idéntica al archivo adjunto. Todavía no existe aprobación explícita del usuario para esta pantalla.

El boceto muestra Música/Cuentos como pestañas internas de Contenido, agregar audio desde el teléfono, reproducción actual en TEO, detener, catálogo con nombre/formato/tamaño, reproducir otra canción y eliminar con confirmación. Cubre el alcance F27–F32 del [inventario](../01-inventario-y-pantallas.md). Los datos son ficticios.

Retoques propuestos para la implementación: evitar que la extensión quede separada del nombre por el salto de línea; mantener Contenido destacado y los destinos inactivos en gris, como en Rutinas; insertar el logo oficial. No se propone agregar edición de metadatos, duración, pausa, avance ni búsqueda de música online.

## Cuentos

Se recibió [esta propuesta de Cuentos](cuentos-propuesta-2026-10-13.png). La copia conserva los bytes del archivo adjunto y todavía no existe aprobación explícita del usuario. Mantiene la estructura de Música y selecciona Cuentos. Incluye agregar PDF, título y cantidad de palabras por cuento, consultar/editar el texto, eliminar y solicitar lectura en TEO, con estado y acción de detener. Corresponde a F33–F38. Se propone unificar la línea indicadora de la pestaña con el azul petróleo de Música durante la implementación.

La acción «Leer con TEO» es una incorporación propuesta al catálogo Android: el [listado actual](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/stories/StoriesFragment.kt) abre la edición y permite eliminar, pero no dibuja ese botón. La API `/api/stories/play` y los [métodos Android playStory/stopStory](../../../ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotConnectionManager.kt) ya existen. No se presenta el botón como implementado.

Para el primer boceto de Cuentos se propone un escenario sin música ni cuento activo, con TEO conectado y despierto. Es un escenario independiente: cambiar de pestaña no debe enviar una orden de detener el audio. El criterio de diseño propuesto es conservar e informar la reproducción vigente al navegar entre pestañas. No se promete pausa o reanudación desde un punto guardado.

## Agregar cuento

Se recibió [esta propuesta de Agregar cuento](agregar-cuento-propuesta-2026-10-13.png), todavía sin aprobación explícita del usuario. Es un formulario dedicado que reúne el PDF elegido, la opción de cambiarlo, el título y las acciones Subir cuento/Cancelar. Reorganiza la selección Android seguida del diálogo de título que usa la beta; no agrega un navegador de archivos propio. El texto «Subir cuento» cumple el mismo propósito del botón «Cargar cuento» solicitado en el prompt.

La [carga actual](../../../ProyectoParaRasperrypiV5/story_library.py) admite PDF con texto de hasta 8 MB, valida el contenido y guarda solo si el resultado es aceptado. El título elegido se limita a 80 caracteres y, si se omite, el servidor lo determina automáticamente. El boceto representa un archivo válido seleccionado y un título completo, antes del envío. No muestra cantidad de palabras extraídas, éxito anticipado ni porcentaje de carga.

## Editar cuento

Se recibió [esta propuesta de Editar cuento](editar-cuento-propuesta-2026-10-13.png), todavía sin aprobación explícita del usuario. Reutiliza el estilo del formulario para editar el título y el texto de un cuento ya cargado, con Guardar cambios/Cancelar. El título es obligatorio y tiene un máximo de 80 caracteres; el texto se muestra en un campo multilínea amplio y desplazable. La beta ya permite editar ambos campos desde un diálogo; la pantalla dedicada reorganiza esa función. Leer con TEO y Eliminar permanecen accesibles en el catálogo. Los cambios deben conservarse en pantalla ante un rechazo o un error de guardado; no se confirma éxito antes de recibirlo. En la implementación debe comprobarse el desplazamiento y la accesibilidad de las acciones al abrir el teclado.

## Agregar música: estado de envío

Se recibió [esta propuesta del estado de envío](musica-envio-propuesta-2026-10-13.png), todavía sin aprobación explícita del usuario. Cubre P11 como un estado de la pestaña Música, sin crear una pantalla de selección de archivos propia. El adulto elige un audio mediante el selector Android existente. Durante el envío se propone un bloque persistente con el nombre del archivo y un indicador indeterminado, deshabilitando Agregar música para evitar duplicados. La nueva canción no se presenta como disponible hasta confirmar la carga. No se inventan porcentaje de progreso, cancelación o pausa del envío. El catálogo y la reproducción existentes conservan sus datos en este escenario. Para la implementación se propone dejar margen entre Sonando en TEO y el catálogo y unificar el indicador de carga con la paleta del resto de la app.
