# TEO Inventario funcional y propuesta de pantallas

Fecha: 10 de octubre de 2026. Estado: propuesta para revisar antes de generar los bocetos. Código de referencia: checkout local `bfe388b`.

## Qué buscamos

Preparar el rediseño de la app Android que utiliza el adulto para acompañar al niño y administrar TEO. Este documento relaciona las funciones encontradas en Android y en la API del peluche con las pantallas que necesitan representarlas. Es la base del posterior paquete de prompts para Gemini o Stitch.

La propuesta conserva las funciones existentes y señala los accesos que faltan. Los nombres y la distribución de las pantallas están abiertos a revisión. La lectura del código permite identificar implementación y contratos; no reemplaza una prueba de funcionamiento en el teléfono y el peluche.

El trabajo de esta etapa comprende el inventario y el mapa de pantallas. Las fichas detalladas, el estilo visual definitivo, los prompts y la implementación se preparan después. El código y el documento Scrum quedan sin modificaciones durante este relevamiento.

## Referencias de identidad recibidas

| Referencia | Uso en el futuro diseño |
|---|---|
| [Logo de TEO](referencias/logo-teo.png) | Logo circular con el nombre TEO y la frase «Inteligencia amigable». Se conserva la imagen entregada. |
| [TEO de frente](referencias/teo.png) | PNG individual agregado por el usuario el 14 de octubre de 2026, con transparencia; fuente actual del personaje. |
| [TEO saludando](referencias/teo-saludando.png) | PNG individual agregado por el usuario el 14 de octubre de 2026, con transparencia; su pose coincide con el Inicio elegido. |
| [TEO y sus skins](referencias/teo-y-skins.png) | Antecedente de identidad: TEO principal es el personaje del centro y las figuras verde y gris son skins. Los dos PNG individuales reemplazan esta foto como fuente del personaje para desarrollar. |

El logo aporta celeste/turquesa, azul petróleo y tonos cálidos de oso como referencias visuales. Esto todavía no fija una paleta, una tipografía ni un tema claro u oscuro. Las fotografías no confirman por sí solas el catálogo físico disponible de skins.

El hardware confirmado por el usuario comprende pantalla, micrófono, cámara y dos servos para los brazos. El pulsador, los sensores de contacto y el movimiento de cabeza están en evaluación. Los bocetos deben respetar esa distinción.

El código ya guarda un campo `costume` en el perfil y permite elegir nueve opciones. Se releva esa función existente; falta confirmar qué opciones se ofrecerán en el producto. Cambiar el disfraz registrado no implica cambiar físicamente el peluche ni disponer de un catálogo de compra. Los valores actuales son oso, perro, oveja, dinosaurio, chancho, tiburón, Stitch, Spiderman y Chimuelo; son el catálogo de software relevado, no una lista de disponibilidad física confirmada.

## Cómo leer el inventario

- **Actual:** hay una función o un recorrido implementado en la app, incluido su trabajo en segundo plano cuando corresponde.
- **Disponible:** existe soporte en la API o los datos, pero el acceso visual indicado necesita agregarse o completarse.
- **Pendiente:** requiere una definición que todavía no se cerró con el equipo.
- **Condicional:** existe en el software, pero depende de hardware cuyo alcance está en evaluación.
- **Técnico:** corresponde a diagnóstico o desarrollo; se propone conservarlo fuera de los controles cotidianos.

Los identificadores F01–F54 permiten comprobar que ninguna función se pierde al generar los bocetos. Los identificadores P01–P20 señalan vistas propuestas; pueden implementarse como pantalla, pestaña, formulario o diálogo. Las fuentes E01–E18 se encuentran al final.

## Inventario de funciones

### Conexión y primer uso

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F01 | Encontrar TEO automáticamente | Actual. Descubrimiento en la red y búsqueda BLE; recuperar la dirección cuando cambia. | P01 / P19 | E01, E03 |
| F02 | Verificar el PIN de vinculación | Actual. PIN de seis dígitos en el recorrido BLE, mostrado por el peluche durante la configuración. | P02 | E03 |
| F03 | Ver y volver a buscar redes Wi-Fi | Actual. Lista de redes vistas por el peluche y selección de una red. | P02 / P19 | E02, E03 |
| F04 | Enviar la contraseña y conectar TEO | Actual. Conexión BLE en el primer uso; estados de conexión y errores. | P02 | E03 |
| F05 | Cambiar la red de TEO | Actual. Configuración contiene cambio de red por HTTP y existe el recorrido de configuración por BLE. | P19 / P02 | E02, E03, E07 |
| F06 | Recuperar una conexión fallida | Actual. Reintentar, buscar de nuevo y abrir los ajustes de Bluetooth o permisos cuando corresponde. | P01 / P02 / P19 | E03 |
| F07 | Solicitar permisos de Bluetooth y notificaciones | Actual. Son permisos del teléfono; su presentación debe integrarse en los recorridos que los necesitan. | P01 / P02 y mensajes del sistema | E03, E04 |
| F08 | Introducir una dirección IP manual | Técnico. Existe una alternativa manual en Configuración. | P20, accesible desde P19 | E07 |

### Perfil del niño y disfraz de TEO

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F09 | Consultar el perfil guardado | Actual. Se carga desde el peluche; el primer uso comprueba si está completo. | P03 | E03, E05 |
| F10 | Guardar o editar los datos del niño | Actual. Nombre, género, intereses y datos adicionales. Nombre y selección de género son necesarios para completar el perfil; existe «Prefiero no decir». | P03 | E05 |
| F11 | Registrar el disfraz o skin utilizado | Actual. El selector se guarda en `costume`; oso es el valor predeterminado. El usuario confirmó el 13/10/2026 conservar las nueve opciones actuales del código. Esto no confirma la disponibilidad física o comercial de cada variante. | P03 | E05 |

### Estado y controles del peluche

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F12 | Consultar conexión y estado de TEO | Actual para conexión y actividad. La API también informa preparación de modelos y despertar pendiente; esas situaciones deben representarse con claridad en el rediseño. | P04 / P19 | E02, E06 |
| F13 | Dormir y despertar TEO | Actual. Suspende o activa la interacción; no corta la alimentación. Despertar desde la app reinicia el contador de límite actual. | P04 | E02, E06, E18 |
| F14 | Celebrar un logro | Actual. Envía una celebración al peluche. La respuesta puede informar que TEO está dormido o que la función no está disponible. | P04 | E02, E06 |
| F15 | Regular el volumen | Actual. Atenuación digital de 0 a 100 %. | P18 | E02, E07 |
| F16 | Regular el brillo visual | Actual. Valor de brillo enviado al peluche y aplicado a su representación visual. | P18 | E02, E07 |
| F17 | Activar o desactivar modo noche | Actual. Activación manual desde la app. | P18; acceso rápido en P04 como propuesta | E02, E07 |
| F18 | Configurar el límite de uso y ver lo restante | Pendiente de definición del equipo. Existe configuración de 0 a 240 minutos; cero significa sin límite. Hoy cuenta tiempo despierto. Se desea interacción efectiva y reinicio por el adulto. | P18 / P04, reservados hasta definir el criterio | E02, E07, E18 |
| F19 | Permitir despertar mediante el pulsador | Condicional. La app y la API exponen el ajuste; el pulsador físico está en evaluación según el usuario. | P18 solo si se confirma ese hardware | E02, E06, E18 |

### Rutinas

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F20 | Consultar las rutinas y su estado | Actual. Nombre, hora, mensaje, activación y marcas de recordada/completada cuando llegan desde el peluche. | P07 | E08 |
| F21 | Crear una rutina | Actual. El formulario expone nombre, hora y mensaje del recordatorio. | P08 | E08 |
| F22 | Editar una rutina | Actual. Edición de esos mismos campos. | P08 reutilizada | E08 |
| F23 | Activar o desactivar una rutina | Actual. Conserva la rutina y sincroniza su habilitación. | P07 / P08 | E08 |
| F24 | Eliminar una rutina | Actual. Actualmente se descubre mediante pulsación prolongada y pide confirmación. | Acción visible en P07 / P08 y confirmación | E08 |
| F25 | Recibir recordatorios en el teléfono | Actual. La app programa avisos locales para rutinas habilitadas, incluido el preaviso. | P07 / P08 y notificación Android | E08, E15 |
| F26 | Personalizar anticipación y mensajes de transición/logro | Disponible en el modelo de datos y el peluche. La UI beta fija cinco minutos de preaviso, deja las transiciones vacías y usa una frase de logro predeterminada. Hacerlo editable amplía el formulario. | Sección opcional en P08, por decidir | E08, E09 |

### Música y cuentos

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F27 | Consultar las canciones cargadas | Actual. Listado, archivo y canción en reproducción. | P09 | E02, E10 |
| F28 | Subir una canción desde el teléfono | Actual. Selección de archivo y transferencia al peluche. | P11 | E02, E10 |
| F29 | Reproducir una canción elegida | Actual. Acción por canción en el listado. | P09 | E02, E10 |
| F30 | Iniciar música desde Inicio | Actual. Usa la última canción elegida si sigue disponible; en caso contrario, la primera del listado. | P04 | E06 |
| F31 | Ver y detener lo que está sonando | Actual. Inicio muestra música o cuento en curso y su acción de detener envía ambas órdenes de parada. El listado de música también permite detenerla. | P04 / P09 / P10 | E02, E06, E10 |
| F32 | Eliminar una canción | Actual. Eliminación del archivo con confirmación en la app. | P09 y confirmación | E02, E10 |
| F33 | Consultar cuentos y abrir su contenido | Actual. Catálogo con título y cantidad de palabras; consulta del texto de un cuento. | P10 / P13 | E02, E10 |
| F34 | Subir un cuento PDF y asignarle título | Actual. El servidor acepta PDF con texto de hasta 8 MB y puede rechazar archivos o contenido. No hay OCR en esta ruta. | P12 | E02, E10, E11 |
| F35 | Editar título y texto de un cuento | Actual. Guardado con validación y posibilidad de rechazo; el título admite hasta 80 caracteres. | P13 | E02, E10, E11 |
| F36 | Eliminar un cuento | Actual. Confirmación y actualización del catálogo. | P10 / P13 y confirmación | E02, E10 |
| F37 | Elegir un cuento del catálogo y pedir que TEO lo lea | Disponible. Existen la API, sus callbacks y los métodos Android. El listado beta abre la edición y ofrece borrar, pero no tiene una acción para iniciar la lectura del cuento seleccionado. | Acción «Leer con TEO» en P10 / P13 | E02, E06, E10, E17 |
| F38 | Consultar el cuento activo y volver a iniciarlo | Actual en Inicio. `currently_reading` informa título e ID y el botón de reproducción vuelve a solicitar ese cuento. No acredita una pausa o reanudación desde un punto guardado. | P04 / P10 | E02, E06 |

### Actividad y avisos

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F39 | Consultar actividad de hoy, semana o mes | Actual. Consultas de uno, siete o treinta días; también existe consulta por fecha en la API. | P05 | E02, E12 |
| F40 | Ver totales de actividad | Actual. Interacciones, juegos, rutinas completadas y eventos de crisis según la telemetría recibida. La existencia del contador no certifica el registro completo de todas las actividades. | Resumen P04 y detalle P05 | E12, E16 |
| F41 | Ver interacciones por día | Actual. Gráfico de barras con los datos del período. | P05 | E12 |
| F42 | Ver distribución por los cuatro pilares | Actual. Emocional, cognitivo, vincular y autonomía. Son conteos de actividad, no puntuaciones de desarrollo del niño. | P05 | E12, E16 |
| F43 | Consultar indicadores de vocabulario | Actual. Palabras registradas y nuevas palabras del período a partir de la telemetría. | P05 | E12, E16 |
| F44 | Consultar la señal emocional disponible | Actual. Inicio deriva una etiqueta del contexto emocional registrado. Presentar su carácter estimado y el estado sin datos. | P04 / P05 | E06, E16 |
| F45 | Ver avisos recientes y recibir notificaciones Android | Actual. Avisos, logros, pedidos y crisis. El listado de Inicio conserva hasta veinte avisos en memoria; no hay un historial completo persistente ni acciones de leído/archivado en la API revisada. | Resumen P04 y vista P06 propuesta | E02, E13 |
| F46 | Mostrar el indicador llamado «Tiempo de juego» | Pendiente de revisión con el equipo. Hoy acumula tiempo de procesamiento de intervenciones; no equivale al reloj del límite ni a duración efectiva de uso. | P04 / P05, sin cerrar cifra ni significado | E06, E12, E18 |

### Agenda y herramientas del teléfono

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F47 | Consultar turnos médicos | Actual. Lista guardada localmente en el teléfono. | P14 | E14 |
| F48 | Crear un turno | Actual. Título, fecha, hora y notas opcionales. No se encontró edición de turnos en la UI ni en el ViewModel. | P15 | E14 |
| F49 | Eliminar un turno | Actual. Pide confirmación. | P14 y confirmación | E14 |
| F50 | Consultar vacunas y cambiar su estado | Actual. Lista precargada y marca aplicada/pendiente; no existe alta libre de vacunas. | P16 | E14 |
| F51 | Programar la fecha de una vacuna pendiente | Actual. Actualmente se abre con pulsación prolongada sobre la fila. | Acción visible en P16 y selector de fecha | E14 |
| F52 | Recibir recordatorios de turnos y vacunas | Actual. Se programan en Android y los datos no se sincronizan con el peluche por esta ruta. | P14 / P16 y notificación Android | E14, E15 |

### Soporte e información

| ID | Función encontrada | Estado y alcance actual | Vista propuesta | Fuente |
|---|---|---|---|---|
| F53 | Consultar información técnica de conexión y servidor | Técnico. IP del teléfono/peluche/servidor y resultado de consultas; `server-info` participa también en la vinculación. | P20 | E02, E07 |
| F54 | Consultar información de la app | Actual. Existe una sección de versión. El rediseño debe tomarla de la versión instalada, evitando un texto fijo desactualizado. | P17 / P20 | E07 |

## Funciones del peluche que no equivalen a controles remotos

Conversación, juegos, yoga, solicitudes de abrazo y reacciones emocionales forman parte de la interacción directa con TEO. Se reflejan en contenidos, avisos o métricas cuando hay datos. En la API revisada no se encontraron operaciones específicas para iniciar un juego o yoga desde el teléfono, mover brazos a voluntad, seleccionar un modelo de IA o cambiar el motor de voz.

Si se decide agregar esos controles parentales, habrá que especificar nuevas acciones y, según el caso, ampliar la API. No se incorporan como botones ya funcionales en los bocetos de esta etapa. Esto no significa que los juegos o el análisis emocional estén incompletos: distingue el comportamiento del peluche de su control desde Android.

Tampoco se encontraron funciones de cuenta de usuario, compra de skins, suscripciones, exportación para profesionales, múltiples perfiles de niños, nivel de batería o video en vivo para la app. No se agregan como requisitos implícitos.

## Propuesta de navegación

El usuario aceptó la navegación A con cuatro destinos principales: Inicio, Rutinas, Contenido y Agenda. Se conserva debajo la comparación inicial como antecedente; la alternativa B ya no es una decisión pendiente para el rediseño.

| Alternativa | Barra inferior | Accesos secundarios | Evaluación |
|---|---|---|---|
| A, aceptada | Inicio · Rutinas · Contenido · Agenda | Actividad desde Inicio; Avisos desde Inicio/campana; Mi TEO desde el engranaje. | Conserva la estructura de cuatro áreas y reúne música y cuentos bajo un nombre orientado a su uso. |
| B | Inicio · Actividad · Rutinas · Contenido · Agenda | Mi TEO desde el engranaje; Avisos desde Inicio/campana. | Da acceso directo a métricas; conviene si el equipo confirma que su consulta es una tarea frecuente. |

«Mi TEO» agrupa ajustes del peluche, conexión y perfil. El acceso técnico queda dentro de Conexión, en una opción avanzada. «Contenido» reemplaza a «Archivos» en los bocetos y reúne Música y Cuentos. Agenda reúne Turnos y Vacunas. Las referencias recibidas y sus correcciones se encuentran en el [índice de bocetos](03-indice-bocetos.md), y los componentes comunes se normalizan en la [guía visual v1](04-guia-visual-comun.md).

### Lista de vistas propuestas

| Vista | Nombre de trabajo | Información y acciones que debe contemplar | Funciones |
|---|---|---|---|
| P01 | Encontrar y conectar TEO | Búsqueda, estado, permisos necesarios, reintentar y acceso a ajustes del teléfono. Si ya hay conexión válida, se salta este recorrido. | F01, F06, F07 |
| P02 | Configurar Wi-Fi | PIN, redes disponibles, contraseña, estados de conexión, error y salida/reintento. Puede resolverse en varios pasos de un mismo recorrido. | F02–F06 |
| P03 | Perfil del niño y su TEO | Nombre, género, intereses, datos adicionales, disfraz, guardar y validaciones. Se reutiliza en primer uso y edición posterior. | F09–F11 |
| P04 | Inicio | Estado de conexión y actividad; dormir/despertar; reproducción actual y detener; celebración; resumen de actividad; avisos relevantes y accesos a sus detalles. Tiempo reservado hasta definir su criterio. | F12–F14, F18, F30–F31, F38, F40, F44–F46 |
| P05 | Actividad | Selector Hoy/Semana/Mes; resumen; interacciones por día; pilares; vocabulario y señales disponibles. Cada etiqueta debe corresponder al período elegido. | F39–F44, F46 |
| P06 | Avisos recientes | Lista de avisos disponibles con texto y tipo, estado vacío y retorno. No presentarla como archivo histórico completo. | F45 |
| P07 | Rutinas | Lista ordenada, hora, mensaje, activación, estado disponible, agregar, editar, eliminar y resultado de sincronización. | F20, F23–F25 |
| P08 | Crear o editar rutina | Nombre, hora, mensaje, guardar/cancelar y errores. Anticipación/transiciones/logro quedarían en un apartado opcional si se aprueba exponerlos. | F21–F22, F26 |
| P09 | Contenido Música | Canciones, reproducción actual, reproducir, detener, agregar y eliminar. | F27, F29, F31–F32 |
| P10 | Contenido Cuentos | Catálogo, título, palabras, abrir/editar, agregar, eliminar y acción propuesta para leer un cuento elegido. | F33, F36–F38 |
| P11 | Agregar música | Elegir archivo con el selector Android, estado de envío, resultado y retorno al catálogo. Puede ser un diálogo o estado dentro de P09. | F28 |
| P12 | Agregar cuento | Seleccionar PDF, título, enviar, procesamiento, aceptación o rechazo y retorno al catálogo. | F34 |
| P13 | Detalle y edición de cuento | Título, texto desplazable, guardar/cancelar, validación; leer y eliminar como acciones claramente separadas de editar. | F33, F35–F37 |
| P14 | Agenda Turnos | Lista con título, fecha/hora y notas; agregar y eliminar; disponibilidad de recordatorios del teléfono. | F47, F49, F52 |
| P15 | Nuevo turno | Título, fecha, hora, notas, guardar/cancelar y validación. Editar un turno existente sería una ampliación, no una función encontrada. | F48 |
| P16 | Agenda Vacunas | Lista precargada, estado aplicada/pendiente, fecha programada y acción visible para cambiarla. | F50–F52 |
| P17 | Mi TEO | Accesos a ajustes, conexión y perfil; información de la app. Puede ser una pantalla breve o un menú. | F09, F12, F54; acceso a P18–P20 |
| P18 | Ajustes de TEO | Volumen, brillo, modo noche y aplicar cambios. Espacio pendiente para el límite. Despertar por pulsador únicamente si se confirma hardware. | F15–F19 |
| P19 | Conexión | Estado, buscar/reconectar, cambiar Wi-Fi, recuperación y acceso a diagnóstico. | F01, F05–F06, F12 |
| P20 | Diagnóstico avanzado | Dirección manual, información técnica disponible y versión real. No ocupa espacio principal de Inicio. | F08, F53–F54 |

Son veinte vistas de trabajo, no veinte pestañas ni veinte diseños independientes. Formularios, pestañas y diálogos comparten estructura y componentes. Los estados conectado/desconectado o vacío/con datos son variantes de una vista.

## Recorridos que deberán poder probarse con los bocetos

1. **Primer uso:** abrir → encontrar TEO → si hace falta, PIN y Wi-Fi → completar perfil si falta → Inicio. El código actual resuelve primero la conexión necesaria para guardar el perfil. No imponer siempre todos los pasos si el peluche ya está configurado.
2. **Consultar TEO:** Inicio → reconocer conectado/dormido/preparándose → despertar si corresponde → observar el estado confirmado.
3. **Crear una rutina:** Rutinas → Agregar → nombre, hora y mensaje → Guardar → mostrar si se sincronizó con el peluche → volver al listado.
4. **Gestionar música:** Contenido/Música → Agregar → selector de archivo → envío → catálogo actualizado → reproducir una canción → detener.
5. **Preparar un cuento:** Contenido/Cuentos → Agregar PDF → título → envío y validación → abrir texto → editar y guardar. El inicio desde el catálogo es el acceso pendiente identificado en F37.
6. **Consultar actividad:** Inicio → Actividad → elegir período → revisar métricas y vocabulario → volver conservando una navegación comprensible.
7. **Atender un aviso:** Inicio/campana → Avisos recientes → leer el aviso disponible. No inventar una llamada telefónica o un botón de intervención automática a partir del aviso.
8. **Cambiar un ajuste:** Mi TEO → Ajustes → modificar → Aplicar → confirmar resultado o mostrar error sin perder el valor ingresado.
9. **Cambiar Wi-Fi:** Mi TEO → Conexión → nueva red y contraseña → conectar → buscar nuevamente a TEO si cambia su dirección → confirmar la recuperación.
10. **Organización médica:** Agenda → crear turno o programar vacuna → guardar en el teléfono → mostrar el dato y el estado de permisos de notificación.

## Estados y condiciones que el diseño debe cubrir

| Situación | Qué tiene que entender el adulto | Consecuencia para el diseño |
|---|---|---|
| Buscando o conectando | La búsqueda está en curso y hay una forma de reintentar o salir cuando corresponde. | Progreso, texto de la etapa y acciones coherentes con esa etapa. |
| Conectado y dormido | Hay conexión; TEO tiene suspendida la interacción. | Separar conexión de sueño y ofrecer despertar. |
| Preparándose / despertar pendiente | Se recibió la solicitud, pero TEO todavía no está listo. | Usar los campos de estado disponibles; no dar por ejecutada una orden solo porque respondió HTTP correctamente. |
| Desconectado | Los controles remotos requieren recuperar la conexión. | Estado claro y acceso a Conexión. La propuesta de acceso a datos locales se trata abajo. |
| Sin canciones, cuentos, rutinas o actividad | Falta contenido o todavía no hay registros. | Mensaje y acción útil; no confundir falta de datos con cero real o con error. |
| Enviando un archivo | El envío o validación todavía no terminó. | Estado ocupado y prevención de envíos duplicados; porcentaje solo si la implementación puede informarlo. |
| Rechazo o error al guardar | El cambio no quedó confirmado y puede corregirse o reintentarse. | Conservar el formulario y explicar el motivo disponible. HTTP 200 puede contener un rechazo o una condición como «TEO está dormido». |
| Eliminación | Qué elemento se va a borrar. | Confirmación identificando canción, cuento, rutina o turno. |
| Sin permisos | Qué función no podrá avisar o conectarse y cómo habilitarla. | Explicación contextual y acceso a ajustes Android cuando corresponde. |

### Datos locales y pérdida de conexión

La agenda médica guarda sus datos en el teléfono. Sin embargo, el gate actual puede bloquear la navegación general cuando no encuentra TEO. **Propuesta por revisar:** permitir consultar y gestionar Agenda durante una desconexión, mientras los controles remotos muestran su indisponibilidad. Esto necesita cambiar el recorrido de navegación; no se presenta como una capacidad ya resuelta de la app.

No se encontró una cola general de cambios offline para perfil, archivos o rutinas. Los bocetos no deben prometer «se enviará automáticamente después» hasta definir e implementar esa función.

## Diferencias que conviene resolver durante el rediseño

Estas observaciones afectan al diseño y a su futura conexión con el código. No son cambios ejecutados en esta etapa.

1. **Lectura de cuentos elegidos:** falta el acceso desde el catálogo, aunque existen las operaciones necesarias. Inicio ya muestra y permite volver a solicitar el cuento activo.
2. **Preparación y sueño:** usar estados reales para distinguir conexión, modelos preparándose y actividad; hoy algunos controles asumen éxito con la respuesta de la solicitud.
3. **Acciones escondidas:** hacer visibles eliminar rutina y programar fecha de vacuna, que hoy dependen de pulsación prolongada.
4. **Rutinas con más opciones:** el backend admite anticipación, transición y mensaje de logro, pero el formulario beta no los expone. Mantenerlo simple o agregar un apartado opcional es una decisión del equipo.
5. **Avisos recientes:** la lista disponible es acotada y no persistente. Un historial completo, filtros, marcar leído o archivar requerirían especificación adicional.
6. **Tiempo:** dejar identificados los dos contadores hasta que el equipo defina su modificación. No reemplazar sus etiquetas por «interacción efectiva» como si el código ya midiera eso.
7. **Agenda local accesible:** revisar el bloqueo general por falta de conexión, para poder aprovechar los datos del teléfono.
8. **Datos de demostración:** la agenda beta precarga turnos de ejemplo y algunas vacunas como aplicadas. Para la app final se propone distinguir el contenido de ejemplo de los datos reales y definir un estado inicial adecuado. No inferir que el niño recibió una vacuna por el valor predeterminado del código.
9. **Período de vocabulario:** hay textos beta que dicen «esta semana» aunque se cambia el período. La ficha visual deberá alinear el texto con Hoy/Semana/Mes.
10. **Pulsador y skins:** el ajuste del pulsador depende del hardware que se confirme. El catálogo de disfraces del código no acredita que todas esas skins estén disponibles físicamente.
11. **Fecha de los avisos:** la API incluye una marca de tiempo, pero el listado Android actual conserva líneas de texto. Mostrar fecha/hora en P06 exige conservar esos datos en el modelo de la app; no basta con dibujarlos en la imagen.

## Límites para los próximos prompts

Los prompts se redactarán después de revisar este inventario y la propuesta de navegación. Deberán incluir los IDs de las funciones de cada vista, textos y controles concretos, estados relevantes y la imagen de referencia elegida.

- Usar el logo entregado y TEO del centro como referencias de identidad; no redibujar ni sustituir el personaje sin acordarlo.
- Conservar todas las funciones actuales asignándoles un lugar. Las funciones disponibles, pendientes o condicionales mantienen su identificación.
- No inventar botones de pausa, avance o reanudación de audio: los controles actuales son reproducir y detener; volver a solicitar un cuento no demuestra reanudación.
- No inventar pantalla de batería, streaming de cámara, login, pagos, varios niños o herramientas clínicas como parte del alcance actual.
- Usar datos ficticios claramente identificados en los bocetos. La documentación y los prompts no necesitan contraseñas, tokens, direcciones reales ni datos de niños.
- Incluir el contenido que queda fuera del primer tramo visible de una pantalla larga y los formularios/confirmaciones asociados. Una imagen del encabezado no acredita la cobertura de la pantalla.
- Dejar pendientes el criterio de tiempo, la guía breve y las capturas definitivas conforme a lo conversado.

## Qué revisar con el equipo

El inventario de implementación se apoya en el código. Estas decisiones de producto permanecen abiertas y no se resuelven por inferencia:

| Tema | Decisión que falta | Qué podemos avanzar mientras tanto |
|---|---|---|
| Opciones de rutina | Exponer anticipación/transiciones/logro o mantener los valores actuales. | Cubrir el formulario básico y reservar un apartado opcional. |
| Acceso sin TEO | Permitir Agenda local mientras se recupera la conexión. | Identificar el cambio necesario en el recorrido de desconexión. |
| Controles parentales adicionales | Si hace falta iniciar juegos o yoga desde Android. | Conservar el alcance remoto encontrado; cualquier nuevo control se registra aparte. |
| Tiempo y pulsador | Comportamiento del tiempo y confirmación del hardware. | Reservar esas funciones y evitar dar por resuelta su implementación. |

## Fuentes del relevamiento

Las referencias apuntan al código revisado. Los números de línea son de esta revisión y pueden cambiar cuando avance la implementación.

| Fuente | Archivos y punto de entrada |
|---|---|
| E01 | [RobotDiscovery.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotDiscovery.kt), [RobotConnectionManager.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotConnectionManager.kt:76) |
| E02 | [RobotApiClient.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotApiClient.kt:99), [api_server.py](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/api_server.py:389) |
| E03 | [OnboardingGate.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/onboarding/OnboardingGate.kt:6), [WifiSetupFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/onboarding/WifiSetupFragment.kt:135), [OnboardingRetryFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/onboarding/OnboardingRetryFragment.kt:40), [TeoBleUuids.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/onboarding/TeoBleUuids.kt:19) |
| E04 | [MainActivity.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/MainActivity.kt:141) y solicitud de notificaciones en la misma clase |
| E05 | [ChildProfile.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/onboarding/ChildProfile.kt:15), [ChildProfileForm.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/onboarding/ChildProfileForm.kt:69), [child_profile.py](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/child_profile.py:60) |
| E06 | [DashboardViewModel.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardViewModel.kt:77), [DashboardFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt:118) |
| E07 | [ConfigFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/config/ConfigFragment.kt:186), [ConfigViewModel.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/config/ConfigViewModel.kt:71) |
| E08 | [RoutinesViewModel.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/routines/RoutinesViewModel.kt:10), [RoutinesFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/routines/RoutinesFragment.kt:86) |
| E09 | [routines.py](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/routines.py:25), [guardado de rutinas](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/api_server.py:643) |
| E10 | [StoriesFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/stories/StoriesFragment.kt:123), [StoriesViewModel.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/stories/StoriesViewModel.kt:35) |
| E11 | [story_library.py](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/story_library.py:205), [edición de cuento en la API](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/api_server.py:945) |
| E12 | [MetricsViewModel.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/metrics/MetricsViewModel.kt:29), [MetricsPeriodParser.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/metrics/MetricsPeriodParser.kt:32), [MetricsFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/metrics/MetricsFragment.kt:50) |
| E13 | [ParentInbox.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/ParentInbox.kt:3), [avisos recibidos por Android](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotConnectionManager.kt:428), [TeoAlertPollService.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/notifications/TeoAlertPollService.kt), [cola de avisos en la API](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/api_server.py:263) |
| E14 | [MedicalViewModel.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/medical/MedicalViewModel.kt:38), [MedicalFragment.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/medical/MedicalFragment.kt:162) |
| E15 | [ReminderScheduler.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/notifications/ReminderScheduler.kt:34), [AppNotificationHelper.kt](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/notifications/AppNotificationHelper.kt) |
| E16 | [telemetry.py](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/telemetry.py:28), [estado publicado](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/api_server.py:95) |
| E17 | [callbacks de reproducción conectados al peluche](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/app.py:152) |
| E18 | [PlaytimeGuard](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/session_policy.py:91), [reinicio al despertar](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/api_server.py:184), [cómputo de telemetría](C:/Users/Santi/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5/workers.py:3179) |

### Cobertura de la API del peluche

Se identificaron 29 rutas por método HTTP en `api_server.py`. Todas quedan relacionadas con una función del inventario o una función técnica. Los marcadores `{id}` representan el tramo variable de fecha, cuento o archivo según la ruta. Esta cobertura no garantiza que cada acción tenga un botón visible hoy.

| Método | Ruta | Funciones relacionadas |
|---|---|---|
| GET | `/api/status` | F12–F19, F31, F38, F44 |
| GET | `/api/server-info` | F01, F53; vinculación interna |
| GET | `/api/telemetry/today` | F39–F44, F46 |
| GET | `/api/telemetry/range` | F39–F43, F46 |
| GET | `/api/telemetry/{id}` | F39–F43, F46; fecha específica disponible en API |
| GET | `/api/routines` | F20 |
| GET | `/api/music` | F27, F30–F31 |
| GET | `/api/stories` | F33 |
| GET | `/api/stories/{id}` | F33, F35 |
| GET | `/api/child-profile` | F09–F11 |
| GET | `/api/notifications` | F45 |
| GET | `/api/wifi/networks` | F03, F05 |
| POST | `/api/config` | F15–F16, F18–F19 |
| POST | `/api/celebrate` | F14 |
| POST | `/api/night-mode` | F17 |
| POST | `/api/power` | F13 |
| POST | `/api/routines` | F21–F24, F26 |
| POST | `/api/music/upload` | F28 |
| POST | `/api/music/play` | F29–F30 |
| POST | `/api/music/stop` | F31 |
| POST | `/api/stories/upload` | F34 |
| POST | `/api/stories/play` | F37–F38 |
| POST | `/api/stories/stop` | F31 |
| POST | `/api/wifi/provisioning` | F05–F06; habilitar o cerrar la sesión BLE desde una conexión existente |
| POST | `/api/wifi/connect` | F04–F05 |
| PUT | `/api/child-profile` | F10–F11 |
| PUT | `/api/stories/{id}` | F35 |
| DELETE | `/api/music/{id}` | F32 |
| DELETE | `/api/stories/{id}` | F36 |

La vinculación BLE utiliza además PIN, listado de redes, credenciales y estados mediante el contrato GATT. Agenda y recordatorios locales se relevaron por separado porque no tienen rutas médicas en esta API.

Antecedente consultado: `docs/superpowers/specs/2026-09-11-android-ui-figma-design.md`. Ese trabajo buscaba trasladar la UI beta a Figma. Aquí se prepara un rediseño y se conserva el antecedente sin modificarlo ni heredar automáticamente sus decisiones de estilo.

## Resultado de esta etapa

Quedan identificadas 54 funciones, una propuesta de cuatro áreas principales, veinte vistas reutilizables y los recorridos/estados que necesitan los futuros bocetos. El siguiente trabajo será revisar esta distribución y redactar las fichas de pantalla y el contexto común para los prompts, incorporando el logo y las referencias de TEO entregadas.
