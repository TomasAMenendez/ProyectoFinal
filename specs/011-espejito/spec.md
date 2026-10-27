# Feature Specification: Espejito en la exposición

**Feature Branch**: `011-espejito`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Agregar a la aplicación de exposición ya existente un juego llamado Espejito. El niño mira la cámara frontal e imita, una por una, las caras feliz, triste, enojado y sorprendido. Cada cara se muestra con su imagen y se reconoce en el propio dispositivo, sin internet, sin guardar el rostro y sin ranking. Las voces y la planilla siguen el mismo camino que los otros juegos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Elegir Espejito (Priority: P1)

Después de escribir su nombre, la persona ve el menú de siempre y una tarjeta nueva: Espejito. Se ve y se toca como las otras. Al tocarla se abre el juego, todavía sin encender la cámara. Sombras, Adivina el objeto, Contar y Piedra, papel o tijera siguen igual.

**Why this priority**: Si el juego no está en el menú, no se puede usar en la exposición. El menú actual no puede romperse para agregarlo.

**Independent Test**: Con un nombre ya ingresado, comprobar que aparece Espejito, abrirlo, volver al menú y abrir cada juego que ya existía.

**Acceptance Scenarios**:

1. **Given** el menú con un nombre ya confirmado, **When** se mira la pantalla, **Then** aparece una tarjeta Espejito, con nombre e imagen, del mismo tipo que las demás.
2. **Given** el menú, **When** se toca Espejito, **Then** se abre su pantalla de entrada y no se pide permiso para la cámara.
3. **Given** cualquiera de los juegos que ya existían, **When** se juega y se vuelve al menú, **Then** sus reglas, voces y registro siguen como antes de agregar Espejito.

---

### User Story 2 - Imitar las cuatro caras (Priority: P1)

La persona toca Comenzar a jugar. Recién ahí se pide la cámara frontal y se la ve como en un espejo, sin deformar la imagen. El juego mira un momento una cara tranquila y después pide, en un orden distinto cada vez, feliz, triste, enojado y sorprendido. Cada una aparece una sola vez, con su imagen grande y su nombre, hasta que la imitación se sostiene un segundo. Entonces felicita, deja pasar a la siguiente y, al terminar las cuatro, muestra un cierre amable.

**Why this priority**: Imitar la cara que se ve en la imagen es el juego. Una mueca de un instante no debe contarse como logro.

**Independent Test**: En un dispositivo con cámara frontal, comenzar, sostener cada una de las cuatro expresiones y comprobar el orden sin repeticiones, la felicitación y el cierre. Esta prueba requiere una persona y una cámara reales.

**Acceptance Scenarios**:

1. **Given** la entrada de Espejito, **When** se toca Comenzar a jugar, **Then** se pide el permiso, se muestra la cámara frontal en espejo y sin sonido, y la imagen llena su zona sin estirarse.
2. **Given** el rostro visible y quieto, **When** pasa un segundo y medio, **Then** esa expresión tranquila queda como referencia personal de la sesión y empieza el primer desafío.
3. **Given** una sesión nueva, **When** se presentan los desafíos, **Then** aparecen feliz, triste, enojado y sorprendido una sola vez cada uno, en un orden al azar.
4. **Given** un desafío, **When** se mira la pantalla, **Then** se ven el título Espejito, una instrucción breve de esa emoción, su imagen grande, su nombre, la cámara, un mensaje de estado y el avance de la imitación.
5. **Given** la cara feliz pedida, **When** se sostiene una sonrisa que supera su umbral y se distingue de las otras tres durante un segundo, **Then** se cuenta un solo acierto, se detiene ese desafío, se felicita y se puede pasar al siguiente.
6. **Given** los cuatro aciertos, **When** termina la sesión, **Then** hay un resumen amable y los botones Volver a jugar y Volver al menú principal.
7. **Given** el resultado de un desafío, **When** se elige continuar, **Then** no se repite esa emoción en la misma sesión.

---

### User Story 3 - Entender qué está pasando (Priority: P1)

Si no se ve la cara, el juego lo dice y no da el desafío por ganado. Si se ve la cara pero no se parece a la imagen, invita a imitarla. Se puede volver a oír la instrucción. Al salir, la cámara se apaga.

**Why this priority**: Un niño pequeño tiene que saber qué hacer, y la cámara no puede quedar mirando después del juego.

**Independent Test**: Tapar la cámara, mostrar otra expresión, pedir que se repita la instrucción, salir al menú y comprobar que la imagen se apagó.

**Acceptance Scenarios**:

1. **Given** la cámara activa sin un rostro, **When** pasa un momento, **Then** se lee "No te veo todavía. Acercate un poquito a la cámara" y no hay acierto.
2. **Given** un rostro que no imita la emoción pedida, **When** se lo muestra, **Then** se lee "¡Probá imitar la carita!" y el avance no llega a completarse por un instante suelto.
3. **Given** un desafío en curso, **When** se pide repetir la instrucción, **Then** se vuelve a decir la frase de esa emoción.
4. **Given** la cámara encendida, **When** se vuelve al menú o se sale del juego, **Then** la imagen se apaga, el análisis se detiene y no sigue una voz de este juego.

---

### User Story 4 - Voces, planilla y juego sin internet (Priority: P2)

Al entrar se oye una bienvenida. Cada emoción tiene su instrucción y, al lograrla, su felicitación. Al completar las cuatro caras se anota una sola vez en la planilla de la exposición, igual que el cierre de los otros juegos. El reconocimiento viaja con la aplicación y no se descarga al empezar.

**Why this priority**: La exposición ya habla y ya anota. Espejito tiene que sumarse a eso, no inventar otro registro ni depender de internet para mirar la cara.

**Independent Test**: Completar una sesión y comprobar una sola felicitación por emoción y una sola fila de Espejito. Cortar la red y comprobar que el juego no pide el reconocimiento afuera.

**Acceptance Scenarios**:

1. **Given** la entrada al juego, **When** se abre la pantalla, **Then** se oye una bienvenida propia de Espejito, preparada de antemano con la misma voz que los otros juegos.
2. **Given** un acierto confirmado, **When** se felicita, **Then** esa felicitación se oye una sola vez y no se repite mientras la cara sigue frente a la cámara.
3. **Given** las cuatro caras logradas, **When** cierra la sesión, **Then** la planilla recibe una sola fila con el nombre de la visita, el juego Espejito y el resultado Ganó.
4. **Given** una salida antes de completar las cuatro, **When** se vuelve al menú, **Then** no se anota una fila por cada mirada de la cámara ni una fila de sesión incompleta.
5. **Given** la red cortada y una entrega que ya trae el reconocimiento, **When** se comienza a jugar, **Then** el análisis de la cara no intenta descargar nada para empezar.

---

### Edge Cases

- Una mueca que aparece un instante y se suelta antes de un segundo no es un acierto. El avance crece mientras la expresión se mantiene y baja si se suelta o si la cara desaparece. No vuelve a cero de un golpe por un solo instante perdido, y tampoco se completa con un solo instante bueno.
- Una puntuación por debajo del umbral de esa emoción no es un acierto, aunque sea la más alta de las cuatro.
- Si otra emoción queda claramente por encima, la pedida no se confirma: la pedida tiene que alcanzar al menos el 85 % de la más alta y, además, su propio umbral.
- La misma sesión no vuelve a pedir una emoción ya lograda. Volver a jugar arma un orden nuevo y mira otra vez la cara tranquila.
- Si el permiso ya estaba aceptado, Volver a jugar no vuelve a pedir la entrada inicial. La cámara sigue disponible y la sesión anterior desaparece.
- Salir a la mitad, durante la calibración o en el resumen apaga la cámara y no deja una sesión a medias en el menú.
- Girar la tablet entre horizontal y vertical no borra la sesión ni apaga la cámara si ya estaba aceptada. En horizontal la imagen y la cámara se ven juntas. En vertical la cara sigue visible junto con la imagen de referencia.
- La imagen de la cámara puede verse invertida como un espejo. Esa inversión no cambia cómo se clasifica la expresión.
- No se guarda video, foto ni dato del rostro, y no se envía la imagen fuera del dispositivo. No hay tabla de posiciones, puntaje competitivo, estrellas para desbloquear ni ranking.
- Permiso rechazado, cámara ausente, cámara ocupada, reconocimiento que no puede prepararse y navegador sin esta capacidad muestran un mensaje claro, una forma de reintentar y una forma de salir. Espejito no ofrece una partida completa sin cámara, porque el juego consiste en imitar frente a la cámara.
- Si durante ocho segundos no se vio ninguna cara, la calibración no empieza el primer desafío: sigue pidiendo que la persona se acerque.
- Si la cara se pierde a mitad de un desafío, el avance baja y el mensaje vuelve a decir que no se la ve. Al reaparecer, se sigue el mismo desafío, sin contarlo como nuevo acierto.
- Un error al mirar un instante de la cámara no cierra el juego: se sigue intentando y, si el análisis queda inutilizable, se ofrece reintentar o salir.
- La entrega de un solo archivo que hoy abre la exposición puede no poder usar la cámara ni cargar el reconocimiento al lado del archivo. Esa entrega conserva los juegos que ya funcionaban ahí. Espejito con cámara se comprueba en una entrega local que ya trae el reconocimiento, sin internet. No se afirma que abrir el archivo suelto encienda la cámara hasta probarlo en el navegador de destino.
- Este juego no modifica el peluche, el servidor ni el piloto de la app del teléfono.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El menú de la aplicación de exposición MUST mostrar una tarjeta Espejito, con el mismo tipo de tarjeta, nombre visible e imagen que los juegos ya presentes.
- **FR-002**: La imagen de cada emoción MUST ser la que ya está en la aplicación con estos nombres: Emocion_Feliz.png, Emocion_Triste.png, Emocion_Enojado.png y Emocion_Sorprendido.png. MUST NOT reemplazarse por dibujos nuevos, emojis ni imágenes traídas de afuera.
- **FR-003**: Abrir el menú o la tarjeta MUST NOT pedir permiso de cámara. El permiso MUST pedirse solo después de tocar Comenzar a jugar.
- **FR-004**: La entrada MUST mostrar una bienvenida breve y el botón Comenzar a jugar.
- **FR-005**: Con la cámara aceptada, la vista MUST usar la cámara frontal cuando exista, sin audio del micrófono, en espejo y sin deformar la imagen. En horizontal, la imagen de referencia y la cámara se ven juntas y grandes. En vertical, el rostro y la imagen de referencia siguen visibles.
- **FR-006**: Cada desafío MUST mostrar, durante todo el intento, el título Espejito, la instrucción de esa emoción, su imagen grande, su nombre, la cámara, el estado del reconocimiento y un avance de qué tan sostenida va la imitación.
- **FR-007**: Las instrucciones visibles y habladas MUST ser: feliz, "¡Poné tu mejor sonrisa!"; triste, "¡Hagamos una carita triste!"; enojado, "¡Mostrame tu mejor cara de enojado!"; sorprendido, "¡Poné cara de sorpresa!".
- **FR-008**: Una sesión MUST pedir las cuatro emociones una sola vez, en orden al azar. MUST NOT repetir una emoción ya lograda en esa sesión.
- **FR-009**: Antes del primer desafío, y solo con el rostro visible, el juego MUST observar un segundo y medio una expresión tranquila y usar esos valores como referencia personal. Si en ocho segundos no hubo rostro, MUST NOT empezar el desafío.
- **FR-010**: El reconocimiento MUST ocurrir en el dispositivo, en vivo, sobre una sola cara y sobre la imagen original de la cámara, no sobre la vista en espejo. MUST reutilizar la lógica de expresiones del archivo de referencia, sin su ranking, sus niveles ni su clasificación de jugadores.
- **FR-011**: Feliz MUST mirar la sonrisa de ambos lados de la boca, con el mismo peso en cada lado, y MUST superar 0,18. Triste MUST combinar el gesto hacia abajo de ambos lados de la boca y el centro de las cejas, con pesos 0,4, 0,4 y 0,2, y MUST superar 0,16. Sorprendido MUST combinar la boca abierta y los dos ojos abiertos, con pesos 0,5, 0,25 y 0,25, y MUST superar 0,10. Enojado MUST combinar las dos cejas hacia abajo y los dos lados de la nariz fruncida, con pesos 0,35, 0,35, 0,15 y 0,15, y MUST superar 0,16.
- **FR-012**: Cada puntuación MUST calcularse como el aumento respecto de la referencia tranquila: lo que no sube cuenta cero. La puntuación de la emoción pedida MUST alcanzar su umbral y, además, al menos el 85 % de la puntuación más alta entre las cuatro. Una puntuación menor al umbral MUST NOT ser un acierto.
- **FR-013**: El acierto MUST confirmarse solo si esa condición se sostiene un segundo. Mientras se cumple, el avance sube. Si la cara se va o la expresión deja de cumplir, el avance baja de forma paulatina. Un instante aislado MUST NOT completar el desafío.
- **FR-014**: Al confirmarse, MUST registrarse un solo acierto, MUST detenerse el análisis de esa ronda, MUST oírse una sola felicitación y MUST verse una respuesta positiva. Las felicitaciones MUST ser: feliz, "¡Qué linda sonrisa!"; triste, "¡Qué carita triste! ¡Lo lograste!"; enojado, "¡Qué cara de gruñón! ¡Muy bien!"; sorprendido, "¡Uau! ¡Qué sorpresa!".
- **FR-015**: Después de un acierto MUST poder pasarse al siguiente desafío. Al completar los cuatro MUST mostrarse un resumen amable, Volver a jugar y Volver al menú principal.
- **FR-016**: Sin rostro MUST mostrarse "No te veo todavía. Acercate un poquito a la cámara". Con rostro y sin la emoción pedida MUST mostrarse "¡Probá imitar la carita!". Esos avisos, la instrucción y el cierre MUST poder oírse con la misma voz ya usada por la exposición, preparada de antemano. Pedir repetir MUST volver a decir la instrucción de la emoción actual.
- **FR-017**: La expresión imitada MUST NOT presentarse como un juicio sobre el niño. Solo indica si se pareció a la cara pedida.
- **FR-018**: Al completar las cuatro caras, la planilla MUST recibir una sola fila, con el nombre de la visita, el juego Espejito y el resultado Ganó, por el mismo camino que el cierre de los otros juegos. MUST NOT anotarse en cada mirada de la cámara, MUST NOT anotarse dos veces el mismo acierto y MUST NOT anotarse si la sesión no se completó.
- **FR-019**: MUST NOT existir ranking, tabla de posiciones, puntaje para competir, desbloqueo por estrellas ni niveles.
- **FR-020**: Permiso rechazado, cámara ausente, cámara ocupada, reconocimiento que no puede prepararse, navegador sin esta capacidad, rostro perdido y análisis que deja de poder mirar la cámara MUST mostrar un mensaje comprensible, un reintento cuando tenga sentido y una salida al menú.
- **FR-021**: Al salir, la cámara MUST apagarse, el análisis y las animaciones MUST detenerse, las voces pendientes de este juego MUST cortarse y lo que el reconocimiento esté usando MUST soltarse. MUST NOT quedar mirando en segundo plano.
- **FR-022**: La cámara MUST usarse solo durante este juego. MUST NOT grabar video, guardar fotos, guardar datos del rostro ni enviar la imagen fuera del dispositivo. La resolución buscada es una imagen clara de cámara frontal; si el equipo no puede darla, el juego igual intenta con la que el equipo ofrezca.
- **FR-023**: El reconocimiento MUST poder empezar sin internet. Los archivos que necesita MUST viajar con la aplicación y MUST NOT descargarse al abrir el juego ni pedirse a un servicio externo.
- **FR-024**: Mientras la cámara está activa, la pantalla MUST seguir respondiendo al tacto. MUST NOT correr dos análisis a la vez ni mirar la cámara tan seguido que una tablet modesta deje de responder.
- **FR-025**: El juego MUST poder usarse en 360×640, 800×1280, 1280×800 y 1366×768, con prioridad horizontal, controles grandes y sin depender de pasar el cursor.
- **FR-026**: Los juegos ya existentes, su menú, sus voces y su registro MUST permanecer. Espejito MUST NOT duplicar otro registro ni otro reproductor de voz.
- **FR-027**: La clasificación, la calibración y la estabilidad MUST poder comprobarse con observaciones de ejemplo, sin cámara. Las pruebas que miran un rostro real MUST quedar identificadas como pendientes de un dispositivo con cámara cuando no haya uno disponible.
- **FR-028**: La comprobación de la experiencia MUST incluir una compilación de producción. La cámara y el reconocimiento sin internet MUST comprobarse en la entrega local que corresponde, y la limitación de abrir un archivo suelto MUST quedar dicha si el navegador no permite la cámara en ese modo.

### Key Entities

- **Emoción**: Feliz, triste, enojado o sorprendido. Tiene una imagen propia, un nombre, una instrucción, una felicitación y una forma de medirse contra la cara tranquila.
- **Desafío**: Una emoción pedida dentro de la sesión, con su avance de cero a completo y un solo acierto posible.
- **Sesión**: Las cuatro emociones en un orden, la referencia tranquila de esa persona y el cierre. Se borra al salir o al volver a jugar.
- **Observación de la cara**: Los movimientos de boca, ojos, cejas y nariz vistos en un momento. Sirve para calibrar y para el desafío en curso. No se guarda como foto, video ni identidad.
- **Fila de la visita**: El nombre, el juego Espejito y Ganó, escrita una sola vez al completar las cuatro caras.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el menú, Espejito se reconoce y se abre en un solo toque, en los cuatro tamaños de referencia, sin encender la cámara desde el menú.
- **SC-002**: Una sesión presenta las cuatro emociones una sola vez y en un orden que puede cambiar al volver a jugar.
- **SC-003**: Con observaciones de ejemplo, una sonrisa, una cara triste, una cara enojada y una cara sorprendida que superan su umbral durante un segundo se cuentan como acierto, y un instante suelto no se cuenta.
- **SC-004**: En un dispositivo con cámara, una persona que sostiene la expresión pedida recibe una sola felicitación antes de 2 segundos desde que el avance se completó.
- **SC-005**: Al volver al menú, la cámara deja de mostrar imagen antes de que el menú quede listo para otro juego.
- **SC-006**: Completar las cuatro caras produce una sola fila de Espejito con resultado Ganó. Salir antes no produce fila.
- **SC-007**: Con la red cortada, el juego con cámara no intenta descargar el reconocimiento para empezar.
- **SC-008**: Después de agregar Espejito, una partida de cada juego ya existente sigue pudiendo jugarse y cerrarse como antes.

## Assumptions

- El cambio vive en la aplicación de exposición que ya existe. No crea otra aplicación y no modifica el proyecto del peluche, el servidor ni el piloto de la app del teléfono.
- La tarjeta del menú usa Emocion_Feliz.png, porque es la cara más fácil de reconocer y no se va a dibujar una imagen nueva.
- La bienvenida hablada es "Vamos a jugar al espejito. Imitá la carita." El cierre hablado, al completar las cuatro, es "¡Muy bien! Imitaste las cuatro caritas." Durante la calibración se dice "Mirá a la cámara, tranquilo, sin hacer ninguna cara."
- Los umbrales y los pesos son los del archivo de referencia TEO-Juegos.html. Se pueden ajustar con pruebas de rostros reales sin cambiar las cuatro emociones, los textos ni la regla del segundo y del 85 %.
- Los movimientos que usa esa referencia, con esos nombres, son mouthSmileLeft, mouthSmileRight, mouthFrownLeft, mouthFrownRight, browInnerUp, jawOpen, eyeWideLeft, eyeWideRight, browDownLeft, browDownRight, noseSneerLeft y noseSneerRight. La puntuación de cada emoción es el promedio ponderado de cuánto subió cada movimiento respecto de la cara tranquila.
- El avance suma tiempo solo mientras la condición se cumple, hasta un segundo. Cuando no se cumple, ese avance vuelve a cero en dos segundos, y cada paso no acredita ni quita más de un cuarto de segundo. Así un instante no gana ni borra el intento.
- La calibración usa al menos cinco observaciones del rostro a lo largo de un segundo y medio. Si hay observaciones pero no se llega a ese tiempo, a los ocho segundos se usa el promedio de lo ya visto.
- La felicitación hablada es el aviso de éxito. No se agrega otro reproductor ni un sonido paralelo si la exposición no lo tiene ya.
- La cámara busca una imagen cercana a 640 por 480 y acepta una menor si el dispositivo no puede darla. Se mira una sola cara. Si la vía más rápida de análisis del propio equipo no está disponible, se usa la vía más simple del mismo equipo, sin salir a internet.
- El análisis no se hace en cada instante si eso traba la tablet: no hay dos análisis juntos y el ritmo queda en el orden de unas diez miradas por segundo.
- La planilla sigue el mismo envío que los otros juegos: nombre, juego y resultado. Espejito solo entra cuando las cuatro caras se completaron, con resultado Ganó.
- Si el archivo único no puede usar la cámara ni el reconocimiento, se conserva para los juegos que ya funcionan ahí. Espejito con cámara se entrega con los archivos del reconocimiento adentro, y esa limitación se dice en la entrega si el navegador no permite la cámara al abrir el archivo suelto.
- Los botones usan el mismo tratamiento de letra que el resto de la exposición. Los textos nuevos conservan los acentos y el español rioplatense del pedido.
