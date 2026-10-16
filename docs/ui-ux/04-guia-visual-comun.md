# TEO: guía visual común v1

Fecha inicial: 13 de octubre de 2026. Actualización de Inicio: 14 de octubre de 2026. Dispositivo principal confirmado por el usuario: Samsung Galaxy S25.

## Base visual validada

El usuario aprobó el Inicio implementado y ajustado en el S25 el 14 de octubre de 2026. La [captura aprobada](validacion/s25-inicio-detalles-2026-10-14.png), `fragment_dashboard.xml` y `teo_home_styles.xml` registran esa base. Sus ajustes prevalecen sobre las cifras iniciales que no se hayan actualizado en ejemplos anteriores. La aprobación visual no cierra las pruebas con el peluche ni las comprobaciones pendientes de texto ampliado y ventana compacta.

Para las pantallas siguientes, el boceto elegido de cada vista define la composición y las proporciones. Inicio aporta la identidad y los componentes ya afinados. Los valores numéricos de esta guía son puntos de partida ajustables, no una plantilla de distribución idéntica para todas las pantallas. Se mantienen controles nativos, accesibilidad y datos reales. La aceptación exige comparar capturas con el boceto, no solo compilar.

## Qué queda fijado y qué se propone

La [última imagen de Inicio elegida por el usuario](bocetos/inicio-principal-elegido-2026-10-13.png) es la referencia principal de composición. Reemplaza como referencia principal al Inicio despierto anterior. Las veinte vistas se reúnen en el [índice](03-indice-bocetos.md).

Son decisiones del usuario: tema claro y cálido, azul petróleo, fondo marfil, tarjetas blancas, globo cian para interacciones, confeti de celebración, cuatro destinos principales y nueve skins. Los valores HEX, tamaños y reglas de componentes de esta guía son una normalización propuesta a partir de esos bocetos. No se presentan como medidas exactas extraídas de las imágenes.

La guía tiene una [muestra visual inicial](guia-visual.html) y [tokens en JSON](teo-ui-tokens.json). El JSON distingue las medidas de partida de los ajustes ya validados en Inicio. Para reutilizar componentes, consultar también los recursos Android actuales; la muestra HTML no sustituye la captura nativa aprobada.

La muestra se revisó en navegador a 1120 y 360 píxeles de ancho, sin desbordamiento horizontal, recursos rotos ni errores de JavaScript. Conserva nueve opciones de skin y sus ejemplos de carga y selección funcionan. Se guardaron la [captura de escritorio](validacion/guia-visual-escritorio.png), la [captura compacta](validacion/guia-visual-360.png) y el [informe de comprobación](validacion/guia-visual-qa.json). Estas comprobaciones corresponden a la muestra de la guía, no a la app Android ni a una prueba realizada en el S25.

Prevalecen, en este orden: instrucciones y correcciones del usuario; reglas funcionales del inventario y notas por grupo; esta guía para componentes compartidos; bocetos como referencia de composición. Los datos ficticios y errores de Gemini se sustituyen por el estado y los datos reales de la app.

## Paleta

| Token | Uso | HEX |
|---|---|---|
| teo_background | Fondo marfil | #F8F5ED |
| teo_surface | Tarjetas y campos | #FFFFFF |
| teo_primary | Acción principal, selección e iconos activos | #255763 |
| teo_primary_pressed | Acción presionada | #1D4650 |
| teo_text | Texto principal | #1D343B |
| teo_secondary | Texto secundario e iconos inactivos | #52636A |
| teo_cyan / teo_on_cyan | Globo y acentos cian | #B6E2E8 / #193F48 |
| teo_gold / teo_on_gold | Celebración y acento dorado | #E8BE73 / #3A2C16 |
| teo_success / teo_on_success | Conectado | #C6E6BD / #245B2C |
| teo_sleep / teo_on_sleep | Dormido | #DEE3F3 / #354866 |
| teo_neutral / teo_on_neutral | Desconectado o estado desconocido | #E3E5E4 / #4C565A |
| teo_disabled / teo_on_disabled | Controles deshabilitados | #E0E3E2 / #596569 |
| teo_outline | Borde de campos y controles secundarios | #788A8E |
| teo_error_bg / teo_error | Error y acción destructiva | #FBE9E7 / #A12E2E |

Usar texto oscuro sobre cian, dorado y fondos de estado. El blanco se reserva para botones azul petróleo o destructivos donde el contraste resulte suficiente. Los textos secundarios tienen color propio, no una opacidad arbitraria que los vuelva ilegibles.

Los pares de texto y fondo de esta propuesta se comprueban mediante luminancia relativa. El objetivo de texto normal es al menos 4,5:1; el informe se guarda en [verificación de contraste](contraste-paleta.json). Esto verifica los colores base, no constituye una auditoría de accesibilidad de una app todavía sin implementar. Referencia: [W3C, contraste mínimo](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

El fondo de la app es marfil liso. La textura y las variaciones de iluminación de los bocetos no se convierten en el fondo general. Para Celebrar un logro, el usuario agregó [fondo-logros.jpg](referencias/fondo-logros.jpg) el 14 de octubre: se usa como decoración dentro de la tarjeta, con borde dorado y texto nativo superpuesto. Esta imagen sustituye al confeti vectorial de preparación. Se conserva su proporción con recorte centrado; su tamaño original no determina la altura de la tarjeta.

## Tipografía

Usar la familia sans serif del sistema Android, inicialmente sans-serif, sin descargas de fuentes. Se propone conservar el comportamiento tipográfico nativo y sus variantes de peso compatibles con minSdk 24.

| Rol | Tamaño | Peso |
|---|---|---|
| Título de pantalla | 28 sp | 700 |
| Título de tarjeta o sección | 18 sp | 700 |
| Título compacto de tarjeta de Inicio | 16 sp | 700 |
| Texto principal y valor de campo | 16 sp | 400 |
| Botón | 14 sp | 500 |
| Texto secundario | 14 sp | 400 |
| Estado, etiqueta y contador auxiliar | 12 sp | 500 |
| Número principal del globo | 40 sp | 700 |

Los tamaños se expresan en sp. Los títulos y botones no se convierten a mayúsculas automáticamente. Mantener español rioplatense, acentos y etiquetas coherentes. Las cantidades y mensajes pueden crecer: no reducir continuamente la letra para que entren en un contenedor fijo.

En el pulido de Inicio del 14 de octubre, los estilos `TextAppearance.Teo.Home.*` y `Widget.Teo.Home.*` distinguen título de pantalla (28 sp), nombre TEO (30 sp), secciones (18 sp en negrita), número (40 sp), cuerpo y botones (15 sp), textos secundarios y accesos (14 sp) y etiquetas (12 sp). El espaciado de letras es normal, sin el tracking heredado de Material. Las tarjetas de Inicio usan 12 dp de padding superior y lateral y 4 dp inferior junto a accesos de al menos 48 dp; el encabezado usa 56 dp. Son ajustes locales de Inicio, sin cambiar los estilos de las pantallas beta.

En la tarjeta de estado, las etiquetas Conectado y Despierto reparten el ancho en partes iguales; sus bordes externos coinciden con los de Dormir TEO. En Hoy con TEO, el texto de la última señal usa todo el ancho de su columna, sin un máximo fijo que provoque saltos de línea anticipados. La cola del globo queda centrada bajo el cuerpo. Con ventana estrecha o texto ampliado se conserva la adaptación por contenido.

## Medidas compartidas

| Elemento | Propuesta |
|---|---|
| Escala de espacios | 4, 8, 12, 16, 24 y 32 dp |
| Margen horizontal de pantalla | 16 dp |
| Padding de tarjeta | 16 dp |
| Separación entre tarjetas | 12 dp |
| Radio de tarjeta | 16 dp |
| Elevación de tarjeta | 2 dp |
| Botón principal o secundario | mínimo 48 dp de alto; radio 24 dp |
| Campo de entrada | mínimo 56 dp de alto; radio 12 dp |
| Icono habitual | 24 dp |
| Área táctil mínima de una acción | 48 × 48 dp |
| Encabezado | mínimo 64 dp, además del inset superior |
| Navegación inferior | mínimo 64 dp, además del inset inferior |
| Globo de interacciones | ancho mínimo 88 dp; crece con el número |
| Etiqueta de estado no interactiva | alto mínimo 28 dp |

Son mínimos o puntos de partida, no alturas rígidas para todo contenido. Las tarjetas de formulario y listados crecen según sus textos. La recomendación de área táctil procede de [Android: accesibilidad](https://developer.android.com/guide/topics/ui/accessibility/apps).

## Componentes reutilizables

| ID | Componente | Regla |
|---|---|---|
| C01 | Tarjeta | Superficie blanca, radio y padding compartidos. Una tarjeta reúne una tarea o información relacionada. |
| C02 | Botón principal | Relleno petróleo, texto blanco, icono opcional. La etiqueta describe la acción: Guardar, Conectar, Reproducir. |
| C03 | Botón secundario | Fondo transparente o superficie, borde petróleo y texto petróleo. No usar enlaces subrayados de estilo web. |
| C04 | Acción destructiva | Rojo, reservada a Eliminar; confirmar con nombre del elemento y Cancelar disponible. |
| C05 | Icono de acción | Vector local de 24 dp dentro de área táctil de 48 dp; descripción accesible. |
| C06 | Etiqueta de estado | Texto más icono o punto. No comunicar conectado/dormido/desconectado únicamente mediante color. |
| C07 | Campo | Etiqueta persistente, valor, ayuda y error asociados. Los campos opcionales se identifican. Los textos largos admiten varias líneas. |
| C08 | Selector de período o pestaña | Selección petróleo; opciones restantes legibles. Conserva una única selección y etiquetas acordes al contenido. |
| C09 | Globo de interacciones | Número y palabra interacciones en un único grupo accesible. Cola decorativa, sin texto dibujado dentro de un PNG. |
| C10 | Encabezado | Principal: logo, título, Avisos y Mi TEO. Secundario: volver y título. Primer uso: título sin salida que omita los requisitos actuales. |
| C11 | Navegación inferior | Inicio, Rutinas, Contenido, Agenda. Seleccionado petróleo; restantes en texto secundario. |
| C12 | Mensaje de resultado | Mensaje breve asociado a la acción; error recuperable conserva los datos escritos. |
| C13 | Fila de archivo | Nombre legible, tipo/tamaño disponibles y acciones explícitas. No romper artificialmente la extensión del archivo. |
| C14 | Aviso | Tipo, icono, fecha si existe y mensaje recibido. Etiquetas Logro, Pedido, Atención y Aviso, sin corchetes. |

Los iconos comparten estilo vectorial Material y grosor visual. Se aprovechan los recursos locales compatibles y se completan los faltantes como vectores. Los emojis de Gemini no se copian como sustituto del sistema de iconos; el confeti queda como ilustración decorativa.

## Estados comunes

| Estado | Presentación y comportamiento |
|---|---|
| Disponible | Acción habilitada y texto normal. |
| Presionado o enfocado | Cambio visible de estado y foco; conservar la etiqueta y el contraste. |
| Deshabilitado | Fondo y texto deshabilitados; no envía acciones. Cuando la causa no es obvia, explicarla: Despertá a TEO… o Conectá a TEO… |
| Enviando | Indicador indeterminado y acción temporalmente deshabilitada para evitar duplicados. No inventar porcentajes. |
| Confirmado | Mostrar el resultado que confirmó el estado real, no solo la existencia de una respuesta HTTP. |
| Error | Mensaje claro, reintento cuando corresponda y campos conservados. |
| Sin datos | Representar ausencia con Sin datos o un guion; cero se reserva a un conteo conocido igual a cero. |
| Datos anteriores | Solo conservar datos realmente disponibles y distinguirlos de una actualización vigente. |

Conexión y actividad son estados distintos: Conectado + Dormido es válido. Un despertar pendiente puede mostrar Preparándose. Un dispositivo desconectado tiene estado de actividad no disponible; no se afirma que sigue despierto.

El modo noche es una función del peluche y no un selector de tema oscuro de la app. La primera versión visual de esta guía es clara. Las variantes nocturnas del tema existente deben revisarse expresamente durante la planificación para que no mezclen accidentalmente la paleta beta con la nueva.

## Inicio como primera comparación visual

Mantener este orden de la [referencia elegida](bocetos/inicio-principal-elegido-2026-10-13.png):

1. Logo, Inicio y accesos Avisos / Mi TEO.
2. Estado de TEO: personaje, nombre, conexión, actividad y Dormir/Despertar.
3. Hoy con TEO: globo de interacciones y última señal emocional registrada.
4. Ahora con TEO: reproducción disponible, iniciar música, detener y Ver contenido.
5. Celebrar un logro: dorado, confeti y texto de apoyo.
6. Avisos recientes y Ver avisos.
7. Navegación inferior con Inicio activo.

El contenido central del boceto es la app. Los grandes márgenes exteriores del lienzo no forman parte del ancho móvil. Tampoco se copia la pantalla entera como una imagen: textos, botones, tarjetas y navegación se construyen con componentes Android.

En el S25, la altura y la densidad reales pueden diferir del boceto. La navegación inferior queda fija sobre su inset; el contenido puede desplazarse, sin comprimir todas las tarjetas para imitar una altura de imagen. El globo, el orden, los pesos visuales y las proporciones relativas se conservan.

Las dos acciones de música pueden compartir fila cuando haya espacio; con texto ampliado o ventana estrecha pasan a una distribución que conserve sus etiquetas y áreas táctiles. No ocultar funciones para sostener una captura.

## Recursos gráficos

| Recurso | Fuente | Situación antes de implementar |
|---|---|---|
| Logo | [PNG transparente](referencias/logo-teo.png), actualizado por el usuario el 14 de octubre | Integrado sin fondo; mantener proporciones y legibilidad. |
| Personaje principal | Nueve parejas originales en [skins](referencias/skins), agregadas por el usuario el 15 de octubre de 2026 | Usar el disfraz confirmado en Perfil. «Derecho» corresponde a la pose de frente; «Idea … 2» a la pose saludando de Inicio. Mantener proporciones y encuadrar el contenido visible. Conservar el logo oficial. |
| Fondo de celebración | [fondo-logros.jpg](referencias/fondo-logros.jpg), entregado por el usuario el 14 de octubre | Integrado como `teo_celebration_background.jpg`, separado del texto nativo. El tamaño lo define el contenido de la tarjeta. |
| Selector de disfraces | Nueve valores confirmados y sus PNG originales | Cuadrícula con imagen de frente y nombre completo; resaltar la elección. Confirmar o cancelar. Dos columnas con texto ampliado, tres cuando el ancho lo permita. La fila del perfil también muestra miniatura y nombre. |
| Iconos | Vectores del proyecto y sistema común | Unificar estilo y completar los necesarios. |

Las nueve parejas reemplazan a los PNG individuales del oso como fuente actual del personaje; estos y la [foto con skins](referencias/teo-y-skins.png) quedan como antecedentes. Las poses muestran ojos abiertos: la pose por sí sola no representa sueño o desconexión. Las etiquetas y acciones siguen reflejando el estado real. Un borrador no cambia el personaje global; producción espera el perfil confirmado y la vista previa conserva una selección simulada separada.

La integración y revisión de estos recursos deben figurar como tareas visibles del primer bloque. Un placeholder solo sirve durante preparación; no da por cumplida la comparación visual final.

## Adaptación a Android y al S25

El proyecto actual usa Kotlin, XML, ViewBinding y Material Components. La propuesta mantiene esa base, con recursos compartidos y estilos, sin una migración de framework.

El proyecto apunta a SDK 36. Deben gestionarse los insets de las barras del sistema y del teclado; no reservar alturas de barra con números fijos copiados de una captura. Referencia: [Android: edge-to-edge en Views](https://developer.android.com/develop/ui/views/layout/edge-to-edge).

Registrar antes de comparar: versión real de Android del S25, tamaño lógico disponible, escala de fuente, escala de pantalla y navegación por gestos o botones. El modelo del teléfono por sí solo no determina el espacio útil de la app.

Usar dp para layout, sp para texto y adaptación por restricciones y contenido. Validar también una ventana compacta de 360 dp y fuente ampliada; evitar alturas fijas en cuerpos de tarjetas o texto cortado. El teclado no puede ocultar el campo activo ni la acción para terminar el formulario.

Los cambios de recursos se planifican para no recolorear accidentalmente pantallas beta todavía sin revisar. Se proponen nombres teo_ y adopción por componentes durante la migración; la asignación global de tema se revisa en el plan.

## Criterios de revisión del primer bloque

- [ ] La versión nativa conserva composición, personaje, confeti y globo del Inicio elegido.
- [ ] Las etiquetas y el orden coinciden; los números ficticios no están fijados en producción.
- [ ] Conectado/despierto, conectado/dormido, preparándose y desconectado muestran acciones coherentes.
- [ ] Música y cuentos reflejan su estado real; Detener solo aparece como acción disponible cuando corresponde.
- [ ] Se manejan carga, ausencia de datos, error y reintento sin anunciar acciones no ejecutadas.
- [ ] Los destinos de cada acceso están definidos en el plan; no quedan botones decorativos o rutas vacías.
- [ ] La captura del S25 permite leer y tocar los controles, sin solapamientos con barras del sistema.
- [ ] La ventana compacta, el texto ampliado, el desplazamiento y el teclado conservan las funciones.
- [ ] Las pruebas existentes pertinentes y la compilación del proyecto mantienen una base verificable.
- [ ] Los ajustes se registran por diferencias concretas antes de extender los componentes a las demás pantallas.

Los comportamientos y excepciones de cada grupo siguen en el [índice y sus notas](03-indice-bocetos.md). Tiempo de uso, pulsador y acceso local sin TEO conservan sus decisiones pendientes. Las correcciones de vocabulario y fecha de avisos están documentadas y no se dan por implementadas en esta guía.
