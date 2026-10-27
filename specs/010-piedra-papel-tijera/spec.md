# Feature Specification: Piedra, papel o tijera en la exposición

**Feature Branch**: `010-piedra-papel-tijera`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Agregar a la aplicación de exposición ya existente un cuarto juego, Piedra, papel o tijera. La persona juega contra la computadora con la mano frente a la cámara o, si no quiere usar la cámara, tocando las ilustraciones de piedra, papel y tijera. El reconocimiento ocurre en el propio dispositivo, sin enviar imágenes a ningún lado, y las ilustraciones salen del material original del peluche sin modificarlo."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Elegir el cuarto juego (Priority: P1)

Después de escribir su nombre, la persona ve el menú de siempre y una cuarta tarjeta: Piedra, papel o tijera. La tarjeta se ve y se toca como las otras tres. Al tocarla se abre ese juego. Sombras, Adivina el objeto y Contar siguen jugándose igual.

**Why this priority**: Si el juego no está en el menú, la exposición no puede usarlo. El menú actual no puede romperse para agregarlo.

**Independent Test**: Con un nombre ya ingresado, comprobar que hay cuatro tarjetas, abrir Piedra, papel o tijera, volver al menú y abrir cada uno de los tres juegos anteriores.

**Acceptance Scenarios**:

1. **Given** el menú con un nombre ya confirmado, **When** se mira la pantalla, **Then** aparecen cuatro tarjetas del mismo tipo: Sombras, Adivina el objeto, Contar y Piedra, papel o tijera, cada una con su nombre y una imagen.
2. **Given** el menú, **When** se toca Piedra, papel o tijera, **Then** se abre la pantalla de ese juego y no se pide permiso para la cámara todavía.
3. **Given** el menú en una tablet horizontal, **When** se muestran las cuatro tarjetas, **Then** las cuatro se alcanzan sin quedar una escondida y sin cambiar el aspecto de las tres que ya existían.
4. **Given** cualquiera de los tres juegos anteriores, **When** se juega una partida y se vuelve al menú, **Then** sus reglas, voces y cierre siguen como antes de agregar el cuarto juego.

---

### User Story 2 - Jugar tocando las figuras (Priority: P1)

Quien no quiere usar la cámara, o no puede, igual juega. En la entrada del juego elige continuar sin cámara. Ve siempre las tres figuras originales, con su nombre debajo, y al tocar una la computadora responde con otra. El resultado dice si ganó, empató o perdió, y puede jugar de nuevo o volver al menú.

**Why this priority**: El juego tiene que poder completarse en la exposición aunque la cámara falle o la persona no quiera mostrarla.

**Independent Test**: Entrar al juego, continuar sin cámara, tocar cada figura y comprobar victoria, empate y derrota contra una jugada conocida de la computadora. Repetir y volver al menú.

**Acceptance Scenarios**:

1. **Given** la entrada del juego, **When** se elige continuar sin cámara, **Then** no aparece el pedido de permiso de la cámara y se puede jugar con las tres figuras.
2. **Given** las tres figuras visibles, **When** se toca una, **Then** esa es la jugada de la persona, la computadora elige recién entonces piedra, papel o tijera con la misma chance para cada una, y se muestra el resultado.
3. **Given** el resultado, **When** se mira la pantalla, **Then** se ven la jugada de la persona, la de la computadora, el nombre de cada una y un mensaje distinto para victoria, empate o derrota.
4. **Given** el resultado, **When** se elige jugar otra vez, **Then** desaparece el resultado anterior y se puede elegir otra figura, con una jugada nueva de la computadora.
5. **Given** el resultado o la partida en curso, **When** se vuelve al menú, **Then** la partida queda limpia y el menú muestra las cuatro tarjetas.

---

### User Story 3 - Jugar con la mano frente a la cámara (Priority: P1)

La persona activa la cámara frontal y se ve como en un espejo. Acerca la mano a una zona indicada. Si mantiene un puño, una palma abierta o la señal de tijera el tiempo suficiente, empieza una cuenta de tres, dos y uno. Si sostiene el mismo gesto hasta el final, ese gesto queda jugado y la computadora responde.

**Why this priority**: Jugar con la propia mano es el motivo del juego. Una detección de un instante no debe disparar la partida.

**Independent Test**: En un dispositivo con cámara frontal, activar la cámara, sostener cada gesto válido y comprobar la cuenta, el bloqueo de la jugada y el resultado. Esta prueba requiere una persona y una cámara reales.

**Acceptance Scenarios**:

1. **Given** la entrada del juego, **When** se toca activar cámara, **Then** recién ahí se pide permiso, se muestra la cámara frontal como un espejo y sin sonido, y empieza a observarse una sola mano.
2. **Given** la cámara activa sin una mano visible, **When** pasa un momento, **Then** se lee "Acercá tu mano a la cámara" y no empieza la cuenta.
3. **Given** una mano con un gesto que no es piedra, papel ni tijera, **When** se la muestra, **Then** se lee "No reconozco ese gesto. Probá con piedra, papel o tijera" y no empieza la cuenta.
4. **Given** un gesto válido que se sostiene, **When** la misma clasificación se mantiene cerca de 800 milisegundos con confianza suficiente, **Then** se lee "¡Te detecté! Mantené el gesto..." y comienza la cuenta 3, 2 y 1.
5. **Given** la cuenta en curso, **When** la mano se va o el gesto cambia, **Then** la cuenta se cancela y vuelve a pedirse un gesto, sin jugada de la computadora.
6. **Given** la cuenta llegó a cero con el mismo gesto, **When** se registra la jugada, **Then** deja de aceptarse otro gesto hasta la próxima ronda, la computadora elige entonces al azar y se muestra el resultado.
7. **Given** la cámara activa, **When** se toca una de las tres figuras, **Then** esa figura vale como jugada inmediata, se cancela la espera o la cuenta, y la computadora responde.

---

### User Story 4 - Salir sin dejar la cámara encendida (Priority: P2)

Al volver al menú, la cámara se apaga y no sigue mirando a la persona. Si el permiso se niega, no hay cámara, está ocupada o el reconocimiento no puede prepararse, se entiende qué pasó y se puede reintentar o jugar con las figuras.

**Why this priority**: La cámara es sensible. Tiene que servir solo durante el juego y el puesto no puede quedar trabado si algo falla.

**Independent Test**: Activar la cámara, volver al menú y comprobar que la imagen se apagó. Repetir rechazando el permiso y comprobando que las figuras siguen jugables.

**Acceptance Scenarios**:

1. **Given** la cámara encendida, **When** se vuelve al menú, **Then** la imagen se apaga, el análisis se detiene y al abrir otro juego la cámara no sigue activa.
2. **Given** un permiso rechazado, una cámara ausente, una cámara ocupada o un reconocimiento que no pudo prepararse, **When** ocurre el problema, **Then** hay un mensaje claro, una forma de reintentar y la opción de seguir jugando con las figuras.
3. **Given** un navegador que no puede usar la cámara o el reconocimiento en vivo, **When** se entra al juego, **Then** se explica la limitación y las tres figuras permiten terminar una partida.

---

### Edge Cases

- Un gesto válido que aparece un instante y cambia antes de los 800 milisegundos no inicia la cuenta y el tiempo acumulado vuelve a cero.
- Una detección por debajo del umbral de confianza no cuenta como gesto válido.
- Durante la cuenta, si falta aunque sea un tramo del mismo gesto, no se registra jugada ni elige la computadora.
- Tocar una figura y mostrar un gesto al mismo tiempo resuelve una sola jugada: vale la figura tocada.
- Jugar otra vez con la cámara ya aceptada no vuelve a mostrar el pedido inicial; la cámara sigue disponible y la ronda anterior desaparece.
- Jugar otra vez después de continuar sin cámara no enciende la cámara por su cuenta.
- Salir a la mitad de la cuenta, o mientras se ve el resultado, apaga la cámara y no deja una partida a medias en el menú.
- Girar la tablet entre horizontal y vertical no borra la ronda ni apaga la cámara si ya estaba aceptada.
- Las tres figuras siguen visibles durante la espera, la cuenta y el resultado.
- La imagen de la cámara puede verse invertida como un espejo; esa inversión no cambia cómo se clasifica el gesto.
- No se guarda video, foto ni dato de la mano, y no se envía la imagen fuera del dispositivo.
- Si la entrega de un solo archivo no puede incluir el reconocimiento, los tres juegos anteriores y la partida por figuras siguen disponibles ahí. El juego con cámara no descarga nada al empezar: queda en una entrega que ya trae consigo lo necesario.
- Este juego no agrega una fila a la planilla de la exposición. Los otros tres juegos siguen anotándose como hasta ahora.
- No se agregan frases habladas nuevas. Las voces de los otros juegos no cambian.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El menú de la aplicación de exposición MUST mostrar una cuarta tarjeta, Piedra, papel o tijera, con el mismo tipo de tarjeta, nombre visible e imagen que las tres existentes.
- **FR-002**: La imagen de esa tarjeta y las tres figuras del juego MUST ser las ilustraciones originales de piedra, papel y tijera ya existentes en el material del peluche. MUST copiarse a la aplicación de exposición y MUST NOT modificarse ni reemplazarse por dibujos nuevos ni por emojis.
- **FR-003**: Abrir el menú o esta tarjeta MUST NOT pedir permiso de cámara. El permiso MUST pedirse solo después de tocar "Activar cámara".
- **FR-004**: La entrada del juego MUST ofrecer "Activar cámara" y "Continuar sin cámara".
- **FR-005**: Sin cámara, las tres figuras MUST ser botones suficientes para completar victorias, empates, derrotas, otra ronda y la vuelta al menú.
- **FR-006**: Con la cámara aceptada, la vista MUST usar la cámara frontal cuando exista, sin audio, en espejo y ocupando la mayor parte de la pantalla en horizontal. En vertical, la cámara queda arriba y las tres figuras abajo. La imagen MUST llenar su zona sin estirarse ni deformarse.
- **FR-007**: Dentro de la cámara MUST haber una guía breve para ubicar la mano, sin taparla ni ocupar el lugar de las figuras.
- **FR-008**: Desde que empieza una ronda hasta que se sale del juego, MUST verse siempre piedra, papel y tijera, cada una con su imagen original y su nombre. Esas figuras MUST seguir siendo botones.
- **FR-009**: Al empezar, y mientras no haya un gesto ni una figura elegida, MUST leerse "¡Prepará tu mano!" y "Mostrá uno de estos tres gestos para jugar."
- **FR-010**: El reconocimiento MUST ocurrir en el dispositivo, en vivo, sobre una sola mano y sobre la imagen original de la cámara, no sobre la vista en espejo.
- **FR-011**: Puño cerrado MUST jugárselo como piedra, palma abierta como papel y la señal de dos dedos como tijera. Esas tres clases son las únicas que pueden iniciar la cuenta.
- **FR-012**: Un gesto MUST confirmarse solo si la misma clase se sostiene cerca de 800 milisegundos y cada observación de ese tramo supera el umbral de confianza. Un cambio, una mano que desaparece o una observación débil MUST reiniciar ese tramo.
- **FR-013**: Recién al confirmar el gesto MUST mostrarse "¡Te detecté! Mantené el gesto..." y la cuenta 3, 2 y 1. Durante la cuenta MUST comprobarse que el gesto sigue siendo el mismo.
- **FR-014**: Si la mano desaparece o el gesto cambia durante la cuenta, la cuenta MUST cancelarse, la computadora MUST NOT elegir y el juego MUST volver a pedir un gesto.
- **FR-015**: Al llegar la cuenta a cero, el gesto de la persona MUST quedar fijo y MUST dejar de aceptarse otro gesto hasta la próxima ronda.
- **FR-016**: La computadora MUST elegir piedra, papel o tijera con la misma probabilidad, y solo después de que la jugada de la persona quedó fija.
- **FR-017**: Las reglas MUST ser las tradicionales: piedra gana a tijera y pierde con papel; papel gana a piedra y pierde con tijera; tijera gana a papel y pierde con piedra; la misma figura empata.
- **FR-018**: El resultado MUST mostrar la jugada de la persona bajo "Vos", la de la computadora bajo "Computadora", el nombre de cada figura y uno de estos mensajes: "¡GANASTE! ¡Muy bien!", "¡EMPATE! ¡Los dos eligieron lo mismo!" o "¡GANÉ! ¡Probá otra vez!".
- **FR-019**: Victoria MUST presentarse en verde, empate en amarillo o un tono neutro, y derrota en rojo, con una animación breve del mismo carácter que el resto de la exposición.
- **FR-020**: "Jugar otra vez" MUST limpiar el resultado, permitir otra jugada de la persona y recién entonces una jugada nueva de la computadora. Si la cámara seguía en uso, el reconocimiento MUST volver a esperar un gesto sin pedir otra vez la entrada inicial.
- **FR-021**: "Volver al menú principal" MUST apagar la cámara, detener el análisis, soltar lo que el reconocimiento esté usando, cancelar la cuenta y olvidar la ronda.
- **FR-022**: Sin mano MUST mostrarse "Acercá tu mano a la cámara". Con una mano y sin uno de los tres gestos MUST mostrarse "No reconozco ese gesto. Probá con piedra, papel o tijera."
- **FR-023**: Permiso rechazado, cámara ausente, cámara ocupada, reconocimiento que no puede prepararse y navegador sin esta capacidad MUST mostrar un mensaje comprensible, un reintento cuando tenga sentido y la vía de las tres figuras.
- **FR-024**: Mientras la cámara está activa, la pantalla MUST seguir respondiendo al tacto. MUST NOT correr dos análisis a la vez ni revisar cada fotograma si eso traba la tablet.
- **FR-025**: El reconocimiento MUST poder empezar sin internet. Los archivos que necesita MUST viajar con la aplicación y MUST NOT descargarse al abrir el juego ni pedirse a un servicio externo.
- **FR-026**: La cámara MUST usarse solo para esta partida. MUST NOT grabar video, guardar fotos, guardar datos de la mano ni enviar fotogramas fuera del dispositivo. Al salir, MUST quedar apagada.
- **FR-027**: El juego MUST poder usarse en 360×640, 800×1280, 1280×800 y 1366×768, en horizontal como prioridad, con botones grandes y sin depender de pasar el cursor.
- **FR-028**: Los tres juegos ya existentes, su menú, su cierre y su registro MUST permanecer. Este juego MUST NOT anotarse en la planilla.
- **FR-029**: La lógica de las reglas y del azar de la computadora MUST poder comprobarse sin cámara. Las pruebas que miran una mano real MUST quedar identificadas como pendientes de un dispositivo con cámara cuando no haya uno disponible.
- **FR-030**: La comprobación de la experiencia MUST incluir una compilación de producción, no solo el servidor de desarrollo.

### Key Entities

- **Figura**: Piedra, papel o tijera. Tiene la ilustración original, un nombre visible y puede ser la jugada de la persona o de la computadora.
- **Ronda**: La jugada fija de la persona, la jugada posterior de la computadora y un resultado de victoria, empate o derrota. Empieza vacía y se borra al repetir o al salir.
- **Observación de la mano**: Una clase vista en un momento, con una confianza. Solo el puño, la palma y la señal de tijera pueden acumular tiempo. Sirve para confirmar el gesto y para vigilar la cuenta; no se guarda después de la ronda.
- **Sesión de cámara**: Apagada, en espera de permiso, activa o impedida. Existe solo mientras este juego está abierto.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el menú, las cuatro tarjetas se reconocen y se tocan en los cuatro tamaños de referencia, y una persona abre Piedra, papel o tijera en un solo toque.
- **SC-002**: Sin cámara, una persona termina una ronda en menos de 15 segundos desde que eligió continuar sin cámara.
- **SC-003**: Las nueve combinaciones de figura contra figura dan el resultado tradicional, comprobadas sin usar la cámara.
- **SC-004**: Un gesto válido sostenido cerca de 800 milisegundos inicia la cuenta; un gesto más breve, uno débil o uno distinto no la inicia.
- **SC-005**: Con el gesto sostenido, el resultado aparece antes de 5 segundos desde que empezó la cuenta de 3.
- **SC-006**: Al volver al menú, la cámara deja de mostrar imagen antes de que el menú quede listo para otro juego.
- **SC-007**: Con la red cortada, una ronda por figuras se completa y el juego con cámara no intenta descargar el reconocimiento para empezar.
- **SC-008**: Después de agregar el cuarto juego, una partida de Sombras, una de Adivina el objeto y una de Contar siguen pudiendo ganarse y perderse como antes.

## Assumptions

- El cambio vive en la aplicación de exposición que ya existe. No crea otra aplicación y no modifica el proyecto del peluche, el servidor ni el piloto de la app del teléfono.
- La tarjeta del menú usa la ilustración original de la tijera, que es la más fácil de reconocer entre las tres, porque no se va a dibujar una imagen nueva.
- El umbral inicial de confianza es 0,7. Se puede ajustar sin cambiar las reglas ni los textos. Cada observación del tramo de confirmación tiene que alcanzar ese umbral.
- Cada número de la cuenta permanece cerca de un segundo. La cuenta completa dura cerca de tres segundos, además de los cerca de 800 milisegundos previos.
- Tocar una figura es una elección deliberada: no espera los 800 milisegundos ni la cuenta. Si había una cuenta o un gesto a medio confirmar, se cancela y vale la figura.
- El reconocedor es un modelo preentrenado de una mano, incluido en la aplicación. Las clases que acepta para jugar son el puño cerrado, la palma abierta y la victoria con dos dedos, también cuando el modelo las nombra Closed_Fist, Open_Palm y Victory, en cualquier combinación de mayúsculas.
- La resolución de la cámara busca una imagen clara en una tablet o un celular, y baja lo necesario si el dispositivo no puede sostener una resolución alta. No se exige una resolución que deje afuera a un equipo modesto.
- Si el archivo único que hoy abre los tres juegos no puede cargar el reconocimiento, ese archivo conserva los tres juegos y la partida por figuras. El juego con cámara se entrega junto con sus archivos auxiliares, sin depender de una descarga al momento de jugar.
- La planilla sigue registrando solo Sombras, Adivina el objeto y Contar. El empate de este juego no entra en ese registro.
- Los botones usan el mismo tratamiento de letra que el resto de la exposición. Los textos nuevos conservan los acentos y el español rioplatense del pedido.
