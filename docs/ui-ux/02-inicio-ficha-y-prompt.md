# TEO Primera pantalla de Inicio y prompt para Gemini

Fecha: 10 de octubre de 2026. Estado: primera propuesta visual para revisar mediante un boceto. Base: [inventario y propuesta de pantallas](01-inventario-y-pantallas.md).

Actualización del 13 de octubre de 2026: el usuario confirmó esta [imagen principal de Inicio](bocetos/inicio-principal-elegido-2026-10-13.png). Se conservan también [Inicio despierto anterior](bocetos/inicio-despierto-referencia-2026-10-13.jpg), [Inicio dormido](bocetos/inicio-dormido-referencia-2026-10-13.jpg) e [Inicio desconectado](bocetos/inicio-desconectado-referencia-2026-10-13.png). Los archivos se copiaron sin alterar las imágenes. El resto de este documento conserva la ficha y el prompt iniciales como antecedente; la referencia principal, la [guía visual común](04-guia-visual-comun.md) y el [índice de bocetos](03-indice-bocetos.md) describen el avance visual.

Actualización de recursos del 14 de octubre de 2026: el usuario agregó [TEO de frente](referencias/teo.png) y [TEO saludando](referencias/teo-saludando.png), ambos con transparencia. Son las fuentes actuales del personaje para desarrollar; la pose saludando coincide con el Inicio elegido. Ya no hace falta recortar el oso de la foto con skins. Las instrucciones del prompt histórico sobre aquella foto se conservan como antecedente y no prevalecen sobre esta actualización.

## Decisiones de partida

El usuario aceptó la navegación con cuatro secciones principales: Inicio, Rutinas, Contenido y Agenda. Actividad y Avisos se abren desde Inicio; el engranaje abre Mi TEO, con Ajustes, Conexión y Perfil.

La dirección visual propuesta para este primer boceto es clara, cálida y legible, con colores tomados del logo y detalles del oso central. El tema claro, la distribución y los componentes se podrán ajustar después de ver la imagen; todavía no constituyen un sistema de diseño definitivo.

Se representa un único estado: TEO conectado y despierto, con registros ficticios disponibles y sin audio en reproducción. Los otros estados serán variantes de esta misma pantalla. No se modificó código.

## Ficha funcional de Inicio P04

La pantalla debe permitir al adulto entender el estado de TEO, usar las acciones cotidianas y abrir el detalle de actividad o avisos. El resumen no reemplaza las pantallas de gestión de contenidos, rutinas, perfil y ajustes.

| Bloque | Contenido en este boceto | Acción y comportamiento previsto | Inventario |
|---|---|---|---|
| Encabezado | Logo, Inicio, campana y engranaje. | Campana abre Avisos recientes; engranaje abre Mi TEO. No se presupone un contador de avisos sin leer. | F45, acceso a P17 |
| Estado de TEO | Imagen del personaje central, conexión y actividad diferenciadas: Conectado / Despierto. | Ver conexión lleva a Conexión. Dormir TEO solicita suspender la interacción; el estado se confirma con la respuesta y las consultas de estado. | F12–F13 |
| Resumen del día | Interacciones y última señal emocional registrada. Datos ficticios: 12 y Tranquilo. | Ver actividad abre P05. La señal es una estimación registrada, no una conclusión clínica ni necesariamente el estado presente del niño. | F40, F44 |
| Reproducción | Nada en reproducción. | Reproducir música utiliza la última canción elegida disponible o la primera del catálogo, como el código actual. Detener aparece deshabilitado mientras no hay reproducción. Ver contenido abre P09/P10. | F30–F31, F38 |
| Celebración | Celebrar un logro y una explicación breve. | Envía la celebración a TEO; informa el resultado real. Dormido o desconectado requiere explicar la condición, evitando dar por ejecutada la acción. | F14 |
| Avisos recientes | Un aviso ficticio de logro y Ver avisos. | Abre la lista acotada de avisos recibidos. No supone historial permanente, fecha conservada en la app ni acciones de archivar/leído. | F45 |
| Navegación inferior | Inicio, Rutinas, Contenido, Agenda. Inicio seleccionado. | Abre cada sección y mantiene nombres e iconos consistentes en los siguientes bocetos. | Propuesta de navegación aceptada |

El límite y el indicador de tiempo siguen pendientes de la conversación con el equipo. No se muestran cifras ni una etiqueta de interacción efectiva en esta primera imagen. Su incorporación se resolverá en una variante posterior. El ajuste de despertar por pulsador se mantiene condicionado a la confirmación de hardware.

El modo noche permanece en Ajustes. No se añade un acceso rápido en Inicio en este primer boceto. Volumen, brillo, perfil y conexión siguen accesibles a través de Mi TEO.

## Cómo usar el prompt

1. Adjuntar a Gemini [el logo](referencias/logo-teo.png) y [la foto de TEO y sus skins](referencias/teo-y-skins.png).
2. Copiar el bloque completo que sigue y pedir la generación de la imagen.
3. Conservar el resultado junto con este prompt. La imagen elegida servirá como referencia visual para las siguientes pantallas.
4. Compartir el resultado en este chat para revisar contenido, legibilidad y coherencia funcional antes de extender el diseño.

## Prompt para copiar

```text
Generá una imagen con un boceto visual detallado de la pantalla de INICIO de una app móvil Android llamada TEO. Quiero evaluar una propuesta de diseño antes de programarla.

CONTEXTO
TEO es un peluche inteligente que interactúa con un niño. Esta app la utiliza el adulto responsable para administrar el peluche y consultar su actividad. La interfaz debe ser cálida, clara y fácil de usar para adultos.

REFERENCIAS ADJUNTAS
La primera imagen es el logo oficial: TEO, Inteligencia amigable. Conservá su identidad y proporciones.
La segunda imagen muestra tres peluches. El TEO principal es exclusivamente el oso dorado del CENTRO. Los otros dos son skins y no deben aparecer en esta pantalla. Usá el personaje central como referencia fiel para una imagen pequeña dentro de la tarjeta de estado, respetando su cara, sus ojos y su forma.

ESTILO PROPUESTO
Tema claro, fondo marfil muy suave, superficies claras, azul petróleo para texto y controles, celeste/turquesa tomado del logo y pequeños acentos dorados del oso. Revisá el contraste para que los textos y controles se lean bien.
Tipografía sans serif legible, iconos simples y consistentes, bordes suavemente redondeados y espacios ordenados. Evitá decoración que compita con la información. La imagen de TEO acompaña al contenido sin dominar toda la pantalla.
Usá español rioplatense y respetá los textos indicados abajo.

ESTADO REPRESENTADO
TEO está conectado y despierto. No hay música ni cuento en reproducción. Hay música cargada y actividad disponible. Todos los datos de ejemplo de este boceto son ficticios.

CONTENIDO OBLIGATORIO, EN ESTE ORDEN
1. Encabezado compacto: logo de TEO a la izquierda, título «Inicio», icono de campana para Avisos e icono de engranaje para Mi TEO. Sin contador de avisos sin leer.
2. Tarjeta de estado: imagen pequeña del oso central, nombre «TEO», indicador «Conectado» y estado «Despierto», claramente diferenciados. Botón principal «Dormir TEO» y acceso secundario «Ver conexión».
3. Resumen «Hoy con TEO»: «12 interacciones» y «Última señal emocional registrada: Tranquilo». Dar jerarquía al número y presentar la señal emocional como información secundaria. Acceso «Ver actividad».
4. Bloque «Ahora con TEO»: texto «Nada en reproducción». Botón «Reproducir música», botón secundario «Detener» deshabilitado y acceso «Ver contenido». Que se entienda que el audio sale del peluche. No dibujes pausa, avance, barra de duración o controles de reproducción que no se pidieron.
5. Acción «Celebrar un logro», con el texto breve «TEO acompaña el festejo». Debe ser fácil de encontrar y distinguir de los controles de reproducción.
6. Bloque «Avisos recientes»: una fila de ejemplo con tipo «Logro» y texto «TEO registró una palabra nueva». Acceso «Ver avisos».
7. Barra de navegación inferior con exactamente cuatro destinos, cada uno con icono y etiqueta: «Inicio», «Rutinas», «Contenido», «Agenda». Inicio está seleccionado. Actividad y Avisos se abren desde los accesos anteriores; no son pestañas adicionales.

ALCANCE
Los ajustes de volumen, brillo, modo noche y perfil se abren desde el engranaje; no agregues sus formularios a Inicio. El criterio de tiempo de uso se está definiendo: no incluyas contadores de tiempo ni inventes sus valores. Tampoco agregues batería, video de cámara, controles de servos, pulsador, login, tienda o compra de skins.

FORMATO DE SALIDA
Entregá UNA sola imagen frontal y plana de esta pantalla, como una captura de interfaz, con proporción aproximada 9:20 y resolución alta, idealmente 1080 × 2400. No incluyas un teléfono físico, perspectiva, collage, variantes ni párrafos explicativos alrededor.
Mostrá todo el contenido solicitado y la navegación inferior, manteniendo textos cómodos de leer y controles utilizables. Reorganizar el espacio y usar dos columnas en el resumen está permitido. No elimines bloques ni reduzcas los textos hasta volverlos ilegibles para hacerlos entrar.
```

## Qué revisar al recibir la imagen

- Están los siete bloques y todos sus accesos; la barra contiene cuatro destinos.
- Se reconoce el logo y el TEO central; las skins no reemplazan al personaje principal.
- Conectado y Despierto se entienden como estados diferentes.
- Dormir, reproducir y celebrar son acciones distintas y visibles.
- Los textos se leen a tamaño de teléfono y las tarjetas no compiten todas con la misma jerarquía.
- No aparecieron funciones inventadas, cifras de tiempo o controles de audio adicionales.

Primero se corrige este boceto y se elige la dirección visual. Luego se preparan las variantes de Inicio dormido, preparándose, desconectado, sin datos y con música o cuento activo, manteniendo el diseño elegido. Esas variantes requerirán sus propios mensajes y acciones habilitadas.

## Prompt breve para una corrección posterior

```text
Editá la imagen de Inicio que adjunto. Conservá la identidad de TEO, los colores, la tipografía, la barra inferior y los componentes que ya están aprobados.
Aplicá únicamente estos cambios:
[Escribir aquí los cambios concretos que acordemos al revisar el boceto.]
Conservá los controles y textos del prompt original salvo los cambios indicados. Entregá una única imagen plana de la pantalla completa.
```

El marcador del último bloque se completa después de revisar la imagen; no forma parte del primer prompt listo para copiar.
