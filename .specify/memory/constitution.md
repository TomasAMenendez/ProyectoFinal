<!--
Sync Impact Report
- Enmienda 2026-10-16: 1.1.0 → 1.2.0. El tiempo de uso deja de estar aplazado y sigue el diseño del 16 de octubre de 2026. Pulsador y acceso a la agenda sin TEO siguen aplazados.
- Version change: plantilla sin ratificar → 1.0.0
- Modified principles:
  - placeholder 1 → I. Conservar el funcionamiento existente
  - placeholder 2 → II. Mantener la base técnica
  - placeholder 3 → III. Respetar las referencias visuales
  - placeholder 4 → IV. Conservar la identidad acordada
  - placeholder 5 → V. Mostrar datos y estados reales
- Added principles:
  - VI. Garantizar una interfaz adaptable
  - VII. Separar las decisiones de producto pendientes
  - VIII. Desarrollar por bloques verificables
  - IX. Verificar antes de declarar terminado
- Added sections:
  - Restricciones vigentes del piloto
  - Revisión por bloques
- Removed sections: ninguna. Se reemplazaron los placeholders de la plantilla.
- Templates: spec, plan, tasks y checklist no copian estos principios.
  Leen la constitución al ejecutarse. No se modificaron.
- Follow-up TODOs: ninguno.
-->

# Constitución de TEO

## Core Principles

### I. Conservar el funcionamiento existente

El rediseño MUST comenzar por la interfaz Android y MUST conservar los contratos y las funciones actuales. Los proyectos del peluche y del servidor MAY consultarse como fuente. Este alcance MUST NOT modificarlos de forma automática.

Rationale: el inventario ya relaciona cada función con una vista. Perder un contrato o cambiar el servidor para acomodar una pantalla rompe el producto que la app ya opera.

### II. Mantener la base técnica

La app MUST seguir en Kotlin, XML, ViewBinding, Fragments, ViewModels y Material Components. Una migración de framework o una actualización de dependencias MUST tener una necesidad concreta y MUST revisarse antes de aplicarse.

Rationale: el piloto tiene que demostrar Inicio sobre el proyecto que ya compila, no abrir una reescritura paralela.

### III. Respetar las referencias visuales

Cada bloque MUST usar `docs/ui-ux/04-guia-visual-comun.md`, sus tokens y el boceto elegido de esa pantalla. Los valores normalizados son la base de diseño. MUST revisarse y MUST ajustarse cuando una captura real aporte evidencia visual.

La interfaz MUST construirse con componentes Android reutilizables, textos y controles reales. Una captura completa MUST NOT usarse como interfaz.

Rationale: la guía fija componentes y medidas de partida. El boceto fija composición. Ninguno de los dos sustituye a los controles nativos ni a los datos de la app.

### IV. Conservar la identidad acordada

La app MUST conservar el logo original, el fondo marfil, el azul petróleo, las tarjetas blancas, el globo cian de interacciones y el confeti de celebración separado del texto.

Las secciones principales MUST ser Inicio, Rutinas, Contenido y Agenda. El perfil MUST conservar las nueve skins existentes.

Las fuentes actuales del personaje MUST ser las nueve parejas PNG originales de `docs/ui-ux/referencias/skins`, entregadas por el usuario el 15 de octubre de 2026. El selector MUST mostrar imagen y nombre. El disfraz guardado y confirmado MUST aplicarse a las imágenes del personaje en la app; el logo oficial MUST conservarse. Su integración MUST respetar proporciones y transparencia, y MUST revisar el tamaño visible y los márgenes. Los PNG anteriores `teo-saludando.png` y `teo.png` se conservan como antecedentes del oso. `docs/ui-ux/referencias/teo-y-skins.png` queda como antecedente y MUST NOT recortarse para fabricar otro personaje.

Rationale: estas decisiones ya fueron confirmadas. Volver a recortar la foto con skins o inventar otra ilustración cambia la identidad acordada.

### V. Mostrar datos y estados reales

Los datos ficticios MUST limitarse a previews y pruebas. La ausencia de datos MUST distinguirse de un cero conocido.

Una respuesta HTTP por sí sola MUST NOT acreditarse como ejecución de una acción en TEO. La interfaz MUST mostrar carga, preparación, confirmación y error según la información real disponible.

La app MUST NOT inventar fechas, historial, capacidades ni garantías que el sistema no proporciona.

Rationale: varios controles actuales tratan la respuesta de la solicitud como éxito. El adulto tiene que ver el estado que el peluche y los datos locales confirman.

### VI. Garantizar una interfaz adaptable

La distribución MUST usar dp y el texto MUST usar sp. La interfaz MUST respetar áreas táctiles, contraste, accesibilidad, texto ampliado, desplazamiento, teclado e insets.

El dispositivo principal de validación MUST ser un Samsung Galaxy S25. El espacio útil MUST NOT asumirse solo por el modelo. Antes de comparar, MUST registrarse la versión de Android, el tamaño lógico, la escala de fuente, la escala de pantalla y si la navegación es por gestos o por botones.

Rationale: el boceto no define la altura útil del teléfono. Comprimir tarjetas para imitar una imagen oculta funciones.

### VII. Separar las decisiones de producto pendientes

El tiempo de uso sigue `docs/superpowers/specs/2026-10-16-tiempo-juego-inicio-design.md`: Inicio muestra el restante y Mi TEO fija el total de 0 a 240 minutos. Pulsador y acceso a la agenda sin TEO siguen aplazados. El piloto MUST NOT resolver esas dos decisiones ni cambiar su comportamiento.

El modo noche del peluche MUST permanecer independiente del tema visual de la app. Durante este piloto, Inicio MUST permanecer claro aunque el teléfono esté en modo oscuro, y ese cambio MUST NOT retematizar de forma global las pantallas beta.

Rationale: el tiempo de uso ya tiene el comportamiento del 16 de octubre de 2026. Pulsador y acceso a la agenda sin TEO siguen sin definirse; mezclarlos con el piloto los presentaría como cerrados.

### VIII. Desarrollar por bloques verificables

Cada bloque MUST preparar y revisar especificación, plan y tareas antes de implementarse. El primer bloque MUST comprender la base visual compartida, Inicio y las dependencias mínimas de navegación ya acordadas.

La navegación temporal hacia Configuración pertenece al piloto. MUST NOT reemplazar el diseño final de Mi TEO, Ajustes, Conexión y Diagnóstico.

Rationale: extender el estilo a las veinte vistas antes de comparar Inicio en el teléfono impide corregir los componentes compartidos.

### IX. Verificar antes de declarar terminado

Antes de declarar un bloque terminado, MUST ejecutarse la compilación y las pruebas pertinentes, y MUST compararse capturas reales con los bocetos. El informe MUST decir qué se comprobó y qué falta.

Compilar MUST NOT tratarse, por sí solo, como prueba de fidelidad visual ni de funcionamiento con el peluche.

Rationale: una app que compila puede conservar la paleta beta, datos ficticios o controles que el peluche no ejecutó.

## Restricciones vigentes del piloto

Inicio MUST usar la pose saludando del disfraz confirmado en Perfil, con Oso como respaldo. La pose de frente MUST estar disponible para la selección y las vistas que la requieran. Ambas muestran ojos abiertos: la pose MUST NOT interpretarse como sueño o desconexión. Etiquetas y controles MUST reflejar el estado real. La apariencia de vista previa MUST permanecer aislada del perfil y de las preferencias de producción.

El confeti MUST ser un recurso decorativo separado del texto. Logo, personaje y confeti MUST revisarse antes de aceptar la fidelidad visual final. Un placeholder MUST NOT contar como comparación terminada.

Los accesos de Inicio MUST resolverse así mientras las pantallas definitivas no existan:

- Ver actividad abre las métricas actuales.
- Ver contenido abre la pantalla actual de archivos, presentada como Contenido.
- El engranaje Mi TEO y Ver conexión abren Configuración.
- La campana y Ver avisos abren una pantalla mínima de avisos recientes.

Detener MUST permanecer visible. MUST estar deshabilitado cuando no haya una reproducción que pueda detenerse, y MUST habilitarse cuando el estado real lo indique.

Los avisos MUST conservar tipo, mensaje y timestamp que ya envía la API. MUST mostrarse el tipo y la fecha u hora cuando el timestamp sea válido. Si no lo es, la fecha MUST omitirse. La lista MUST conservar el límite de veinte avisos en memoria. MUST NOT agregarse historial persistente ni acciones de leído, eliminar o archivar. La pantalla mínima MUST leer la lista ya retenida por la app y MUST NOT volver a vaciar la cola del peluche.

Los textos de interfaz MUST usar español rioplatense, con acentos y sin convertir títulos o botones a mayúsculas de forma automática.

## Revisión por bloques

El orden de cada bloque MUST ser especificación, aclaración cuando haga falta, plan, tareas y recién entonces implementación. La implementación MUST limitarse a las tareas del bloque revisado.

La comparación visual de Inicio MUST hacerse con una captura del Samsung Galaxy S25 y con una ventana compacta de 360 dp. Las diferencias MUST registrarse antes de extender los componentes al resto de las pantallas.

Las decisiones aplazadas y las vistas fuera del bloque activo MUST permanecer identificadas y MUST NOT implementarse de forma silenciosa.

## Governance

Esta constitución prevalece, para el rediseño Android de TEO, sobre prácticas genéricas de la plantilla de Spec Kit. Los documentos de `docs/ui-ux/` informan el diseño. Si una frase anterior contradice una decisión ratificada aquí, prevalece esta constitución hasta que una enmienda la cambie.

Una enmienda MUST documentar el cambio, MUST indicar su versión y MUST contar con la aprobación de quien mantiene el proyecto antes de modificar el trabajo en curso. La guía de ejecución es `.specify/memory/constitution.md`. La referencia visual común es `docs/ui-ux/04-guia-visual-comun.md`.

El versionado MUST seguir estas reglas:

- MAJOR: se elimina o se redefine un principio de forma incompatible con el anterior.
- MINOR: se agrega un principio o una sección, o se amplía una regla de manera material.
- PATCH: se aclara redacción sin cambiar la regla.

Cada especificación, plan y revisión MUST comprobar el cumplimiento de estos principios. Una excepción MUST justificarse de forma explícita en el plan. Complejidad nueva, dependencias y pantallas fuera del bloque MUST justificarse antes de incorporarse.

**Version**: 1.2.0 | **Ratified**: 2026-10-14 | **Last Amended**: 2026-10-16
