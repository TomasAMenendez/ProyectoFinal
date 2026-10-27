# Feature Specification: Juegos de exposición en tablet

**Feature Branch**: `009-app-expo-juegos`

**Created**: 2026-10-25

**Status**: Draft

**Input**: User description: "Aplicación web independiente para una tablet de exposición, en la carpeta AppParaExpo, junto a ProyectoParaRasperrypiV5 y sin modificar ese proyecto. Tres juegos jugables con los catálogos y dibujos originales: Sombras, Adivina el objeto y Contar. Recorrido con nombre del niño, menú, vidas, voz preparada de antemano y dos entregas del mismo juego: sitio estático publicable y un único archivo que se abre sin internet. El registro en planilla corresponde solo a la versión publicada."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entrar a la exposición (Priority: P1)

Una persona de la exposición abre la aplicación en la tablet. Antes del menú, la aplicación pide el nombre del niño. Con el nombre ingresado aparecen tres opciones grandes: Sombras, Adivina el objeto y Contar. El logo de TEO se reconoce y la pantalla se entiende en horizontal sin buscar controles escondidos. Desde cualquier juego se puede volver al menú.

**Why this priority**: Sin este recorrido nadie llega a jugar. Es la puerta de la exposición.

**Independent Test**: Abrir la aplicación, escribir un nombre, entrar a cada juego y volver al menú. Se comprueba en horizontal y en vertical.

**Acceptance Scenarios**:

1. **Given** la aplicación recién abierta, **When** todavía no hay un nombre, **Then** se pide el nombre y el menú de juegos no aparece.
2. **Given** el pedido de nombre, **When** se confirma un nombre con al menos una letra visible, **Then** aparece el menú con tres tarjetas grandes: SOMBRAS, ADIVINA EL OBJETO y CONTAR.
3. **Given** el menú, **When** se toca una tarjeta, **Then** se abre ese juego y el nombre de la visita se conserva.
4. **Given** un juego abierto, **When** se elige volver al menú, **Then** se ve el menú y la partida abandonada no conserva errores, pistas ni selecciones.
5. **Given** una tablet horizontal, **When** se recorre el menú y un juego, **Then** las acciones importantes se ven juntas, con botones grandes, sin depender de pasar el cursor por encima y sin que la página se desplace al tocar.

---

### User Story 2 - Jugar a Sombras (Priority: P1)

El niño relaciona cinco siluetas con sus cinco nombres. Puede unir arrastrando el dedo o tocando primero la silueta y después la palabra. Las uniones correctas quedan dibujadas. Una unión incorrecta se rompe, cuesta una vida y ofrece una pista de esa silueta.

**Why this priority**: Sombras es uno de los tres juegos que la exposición debe poder jugarse de verdad.

**Independent Test**: Iniciar Sombras, resolver una pareja de cada manera, fallar una pareja y completar o agotar las vidas.

**Acceptance Scenarios**:

1. **Given** una partida nueva, **When** aparece el tablero, **Then** hay cinco siluetas y exactamente sus cinco nombres, sin nombres repetidos, y los nombres no quedan en el mismo orden que las siluetas.
2. **Given** el tablero en horizontal, **When** se mira la partida, **Then** las siluetas están del lado izquierdo y los nombres del lado derecho.
3. **Given** el tablero en vertical, **When** se mira la partida, **Then** las siluetas están arriba y los nombres abajo.
4. **Given** una silueta sin resolver, **When** se mantiene el dedo o el mouse y se suelta sobre su nombre, **Then** una línea queda uniendo ambos y esos dos elementos dejan de poder elegirse.
5. **Given** una silueta seleccionada con un toque, **When** se toca su nombre, **Then** la unión correcta aparece de inmediato y ambos quedan resueltos.
6. **Given** una silueta, **When** se la une con un nombre que no es el suyo, **Then** la línea se rompe con una animación breve, se pierde una vida de derecha a izquierda, se lee "No era esa, te doy una pista" y debajo aparece una pista de esa silueta.
7. **Given** la primera falla de una silueta, **When** se muestra la pista, **Then** es la primera pista de esa ficha. **Given** otra falla de la misma silueta, **When** se muestra la pista, **Then** se sigue el orden primera, tercera, segunda, salteando una pista que esa ficha no tenga.
8. **Given** las cinco parejas unidas, **When** se confirma la última, **Then** la partida se gana. **Given** la tercera unión incorrecta de la partida, **When** se consume esa vida, **Then** la partida se pierde.

---

### User Story 3 - Adivinar el objeto (Priority: P1)

El niño escucha y lee pistas y elige el objeto entre cinco nombres. No ve el dibujo del objeto mientras todavía puede responder. Cada error desactiva esa opción, muestra otra pista y cuesta una vida. Acertar gana en el momento.

**Why this priority**: Es el segundo juego completo que la exposición debe ofrecer.

**Independent Test**: Jugar una ronda, fallar dos veces viendo cómo se acumulan las pistas y acertar; en otra ronda agotar las tres vidas.

**Acceptance Scenarios**:

1. **Given** una partida nueva, **When** aparece el desafío, **Then** se ve la primera pista, hay cinco nombres grandes, uno es la primera respuesta válida del objeto y los otros cuatro son nombres de objetos distintos, en un orden mezclado.
2. **Given** una partida en curso, **When** el niño todavía no terminó, **Then** el dibujo del objeto buscado no se muestra.
3. **Given** una opción incorrecta disponible, **When** se elige, **Then** esa opción queda apagada y no vuelve a contar como intento, aparece la siguiente pista debajo de las anteriores y se pierde una vida.
4. **Given** las pistas de la ficha, **When** avanzan los errores, **Then** el orden de aparición es primera, tercera y segunda. Las pistas ya mostradas siguen a la vista.
5. **Given** la opción correcta, **When** se elige, **Then** la partida se gana en ese momento. **Given** el tercer error, **When** se consume esa vida, **Then** la partida se pierde.

---

### User Story 4 - Contar (Priority: P1)

El niño mira el dibujo, lee la pregunta del catálogo y elige un número del 1 al 10. El número correcto es el que indica la ficha, no una cantidad estimada mirando el dibujo. Un número incorrecto se apaga y cuesta una vida.

**Why this priority**: Es el tercer juego completo de la exposición.

**Independent Test**: Abrir Contar, comprobar pregunta, imagen y botones del 1 al 10, fallar un número y después acertar o agotar las vidas.

**Acceptance Scenarios**:

1. **Given** una partida nueva, **When** aparece el ejercicio, **Then** se ven la imagen de esa ficha, su pregunta y los números del 1 al 10 en orden, en botones grandes.
2. **Given** el catálogo de contar, **When** se evalúa la respuesta, **Then** el número correcto es la primera respuesta de esa ficha.
3. **Given** un número incorrecto todavía disponible, **When** se elige, **Then** se apaga, no vuelve a descontar una vida y los demás números siguen disponibles.
4. **Given** el número correcto, **When** se elige, **Then** la partida se gana. **Given** el tercer número incorrecto, **When** se consume esa vida, **Then** la partida se pierde.

---

### User Story 5 - Cerrar la partida y volver a jugar (Priority: P1)

Al ganar o perder, los tres juegos muestran el mismo tipo de cierre. Ganar se ve en verde, con un mensaje alegre y confeti. Perder se ve en rojo, con la invitación a intentar de nuevo. Las dos acciones son volver a jugar esa misma actividad o volver al menú.

**Why this priority**: La exposición necesita un final claro y una forma segura de pasar al siguiente niño o al siguiente juego.

**Independent Test**: Forzar una victoria y una derrota en cada juego y usar las dos acciones.

**Acceptance Scenarios**:

1. **Given** una partida ganada, **When** aparece el cierre, **Then** predomina el verde, se lee "¡Muy bien! ¡Ganaste!", hay confeti y el texto sigue pudiéndose leer.
2. **Given** una partida perdida, **When** aparece el cierre, **Then** predomina el rojo y se lee "¡Se terminó el juego! ¡Intentalo otra vez!".
3. **Given** el cierre, **When** se mira la pantalla, **Then** el aviso es notorio y deja ver que el juego terminó, sin tapar toda la pantalla de forma innecesaria, y ofrece VOLVER A JUGAR y VOLVER AL MENÚ PRINCIPAL.
4. **Given** el cierre, **When** se elige volver a jugar, **Then** la misma actividad empieza de nuevo, con tres vidas y un desafío nuevo cuando el catálogo lo permite.
5. **Given** el cierre o la partida, **When** se vuelve al menú, **Then** el juego anterior no conserva vidas perdidas, pistas reveladas ni opciones elegidas.
6. **Given** cualquier partida, **When** se pierde la primera vida, **Then** se ve el corazón de la derecha romperse. **When** se pierde la segunda, **Then** el del medio se rompe y el de la derecha queda vacío. **When** se pierde la tercera, **Then** se rompe el de la izquierda y la partida termina.

---

### User Story 6 - Escuchar a TEO mientras se juega (Priority: P1)

Las frases del juego ya están grabadas con la voz Micaela, la misma voz que usa el servidor de la PC. Durante la exposición la aplicación reproduce esas grabaciones y no vuelve a pedirlas una por una. Hay bienvenida, pistas, la opción elegida, reacciones distintas para acierto y error, y frases distintas al repetir o al cambiar de juego.

**Why this priority**: La exposición es hablada. Una visita no puede depender de generar la voz en el momento ni de repetir siempre la misma frase.

**Independent Test**: Recorrer un acierto, un error, una opción ya descartada, el cierre y las dos salidas, y comprobar que suena la frase correspondiente. Repetir la situación y comprobar que las frases variables no son siempre la misma.

**Acceptance Scenarios**:

1. **Given** las grabaciones preparadas, **When** un niño juega con o sin internet, **Then** las voces salen de la propia aplicación.
2. **Given** la apertura de un juego, **When** entra la partida, **Then** suena una bienvenida de ese juego.
3. **Given** una pista o la pregunta de Contar, **When** aparece por primera vez, **Then** se escucha ese texto.
4. **Given** una opción elegida, **When** se confirma el toque, **Then** se escucha esa opción y después una reacción de acierto o de error.
5. **Given** varias partidas, **When** se acierta, se falla, se vuelve a jugar o se vuelve al menú, **Then** esas reacciones tienen varias formulaciones y no se repite siempre la misma.
6. **Given** una opción ya descartada por incorrecta, **When** se vuelve a tocar, **Then** no se pierde otra vida y se escucha una frase del estilo de probar con otra.
7. **Given** la versión publicada recién abierta, **When** pide el nombre, **Then** se escucha "Hola!, soy Teo, cuál es tu nombre?". **Given** el archivo sin internet, **When** pide el nombre, **Then** esa frase de apertura no es un requisito.

---

### User Story 7 - Publicar la exposición y también llevarla sin internet (Priority: P1)

La misma experiencia se entrega de dos maneras. Una se abre con una dirección web, por HTTPS, en el dominio o dentro de una carpeta del sitio. La otra es un solo archivo que se copia a la tablet, se abre con doble clic y funciona sin internet, sin instalar programas y sin un servidor. Los juegos, los dibujos, las voces y las reglas coinciden.

**Why this priority**: La exposición tiene que funcionar aunque la red del lugar falle, y también tiene que poder publicarse.

**Independent Test**: Generar las dos entregas desde el mismo origen. Abrir la publicada por su dirección. Abrir el archivo local con la red desactivada y jugar los tres juegos.

**Independent Test adicional**: Confirmar que ProyectoParaRasperrypiV5 no cambia.

**Acceptance Scenarios**:

1. **Given** el origen único de la aplicación, **When** se prepara la versión publicada, **Then** queda un sitio estático con los tres juegos, sus datos y sus recursos, utilizable por HTTPS en un dominio o en un subdirectorio.
2. **Given** ese mismo origen, **When** se prepara la versión sin internet, **Then** existe un único archivo, AppParaExpo-Offline.html, que contiene la aplicación, los estilos, los datos, las imágenes y las voces.
3. **Given** ese archivo en el almacenamiento del equipo, **When** se abre con doble clic y sin internet, **Then** se llega al menú, se juegan los tres juegos, se ven las imágenes, suenan las voces incluidas y no se intenta descargar nada.
4. **Given** cualquiera de las dos entregas, **When** se comparan las reglas y el contenido, **Then** son la misma experiencia.
5. **Given** el proyecto original, **When** la aplicación de exposición ya está creada, **Then** ningún archivo de ProyectoParaRasperrypiV5 fue modificado, borrado ni reemplazado.

---

### User Story 8 - Anotar el resultado de la visita (Priority: P2)

Cuando un niño termina una partida en la versión publicada, queda anotada una fila con su nombre, el juego, si ganó o perdió, y la fecha y hora. La versión de un solo archivo no anota ni envía resultados. Si la planilla no está disponible, el juego igual termina.

**Why this priority**: Sirve para mirar la exposición después. No impide jugar.

**Independent Test**: Terminar una partida ganada y otra perdida en la versión publicada y ver las filas. Terminar una partida en el archivo local y comprobar que no se envía nada.

**Acceptance Scenarios**:

1. **Given** la versión publicada configurada, **When** una partida termina en victoria o derrota, **Then** se agrega una fila con nombre, juego, resultado y fecha y hora.
2. **Given** una partida abandonada antes del cierre, **When** se vuelve al menú, **Then** no se agrega una fila.
3. **Given** el archivo sin internet, **When** se termina una partida, **Then** no se anota ni se intenta enviar el resultado.
4. **Given** la planilla inaccesible o sin configurar, **When** la partida termina, **Then** el niño ve el cierre igual y puede seguir.

### Edge Cases

- Soltar una unión de Sombras fuera de un nombre, sobre otra silueta o sobre un elemento ya resuelto cancela la línea y no descuenta una vida.
- Elegir otra silueta reemplaza la selección anterior y no descuenta una vida.
- Tocar una pareja ya resuelta no hace nada.
- Volver a tocar una opción ya descartada por incorrecta no descuenta otra vida.
- Dos toques muy seguidos sobre la misma opción cuentan como una sola jugada.
- Girar la tablet durante la partida conserva vidas, pistas, uniones y opciones descartadas.
- Volver a jugar no repite el mismo ejercicio de Contar, el mismo objeto de Adivina ni las mismas cinco siluetas cuando existe otra combinación.
- Sombras tiene 54 fichas y cada una tiene dos pistas. La tercera pista del orden pedido no existe: se saltea y se usa la segunda pista en la falla siguiente de esa silueta.
- Adivina tiene 67 fichas, todas con tres pistas y con una primera respuesta distinta. Al empezar se ve la primera; el primer error agrega la tercera; el segundo error agrega la segunda.
- Contar tiene 10 ejercicios, uno por cada cantidad del 1 al 10, con imagen y pregunta. Esos números se muestran siempre completos y en orden.
- La ficha del delfín pide la silueta "Silueta_Delfin.png", y el dibujo existente se llama "Silueta_Delfín.png". La aplicación nueva reutiliza ese dibujo existente. No se inventa otra silueta ni se edita el catálogo original.
- Si faltara un dibujo o una ficha imprescindible para armar una partida, ese elemento no entra en el sorteo. Si con lo disponible no se puede armar el juego, se explica con claridad y el resto de los juegos sigue disponible.
- Un nombre vacío o formado solo por espacios no abre el menú. El nombre visible se recorta y se limita a un largo razonable para la pantalla y la planilla.
- Recargar la página vuelve a pedir el nombre. No hay cuentas ni historial en el dispositivo.
- Una grabación que no pueda reproducirse no frena la partida ni el cierre.
- El cierre muestra el dibujo del objeto de Adivina recién cuando la partida ya terminó, para que el niño vea qué era.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La aplicación vive en la carpeta AppParaExpo, al lado de ProyectoParaRasperrypiV5. El proyecto original permanece intacto y la aplicación nueva funciona con los recursos guardados dentro de su propia carpeta.
- **FR-002**: Los juegos usan los catálogos sombras.json, adivina.json y contar.json y los dibujos originales de esos juegos. No se reemplazan fichas ni dibujos por contenido inventado.
- **FR-003**: Antes del menú, la visita indica el nombre del niño. El menú ofrece solo Sombras, Adivina el objeto y Contar, cada uno con nombre, una presentación visual reconocible y un toque claro para entrar.
- **FR-004**: Cada juego puede volver al menú. Al hacerlo, o al terminar y salir, se descarta el estado de esa partida.
- **FR-005**: La prioridad visual es la tablet horizontal. La misma visita también puede usarse en computadora y en celular o tablet vertical. El contenido de juego importante cabe en la pantalla visible, con zonas de toque amplias, texto legible a cierta distancia, buen contraste, animaciones breves y sin desplazamiento accidental de la página.
- **FR-006**: La interfaz usa el logo oficial de TEO y se reconoce como un juego educativo infantil de TEO. Las acciones se completan con el dedo. El mouse también puede jugar Sombras y elegir opciones.
- **FR-007**: Cada partida de Sombras presenta cinco parejas distintas, con los nombres mezclados. En horizontal, siluetas a la izquierda y nombres a la derecha. En vertical, siluetas arriba y nombres abajo.
- **FR-008**: En Sombras se puede unir arrastrando desde la silueta hasta el nombre, con una línea que sigue el dedo, o tocando la silueta y después el nombre. La unión correcta permanece y bloquea esos dos elementos. La unión incorrecta desaparece con una ruptura breve.
- **FR-009**: Una unión incorrecta de Sombras muestra el texto "No era esa, te doy una pista" y una pista de la silueta elegida. Para cada silueta, las pistas siguen el orden primera, tercera, segunda, ignorando las que no existan.
- **FR-010**: Adivina el objeto muestra las pistas recibidas y cinco nombres. La respuesta correcta es solo la primera respuesta válida de la ficha. Los otros cuatro nombres pertenecen a otros objetos. El dibujo del objeto permanece oculto hasta que la partida termina.
- **FR-011**: En Adivina, la primera pista está visible al empezar. Cada error agrega la pista siguiente del orden primera, tercera, segunda, conserva las anteriores y deja inutilizable la opción elegida, con una apariencia apagada.
- **FR-012**: Contar muestra la imagen y la pregunta de la ficha, y los números del 1 al 10 en orden. La cantidad correcta es la primera respuesta de esa ficha.
- **FR-013**: En Adivina y en Contar, una opción incorrecta elegida queda inutilizable. Volver a tocarla no consume otra vida.
- **FR-014**: Los tres juegos dan tres vidas por partida. Cada error nuevo consume una, rompiendo un corazón de derecha a izquierda. La tercera vida perdida termina la partida. Acertar el objetivo del juego la gana: las cinco uniones en Sombras, el nombre correcto en Adivina o el número correcto en Contar.
- **FR-015**: El cierre de victoria usa el verde, el mensaje "¡Muy bien! ¡Ganaste!" y confeti que no impide leer. El cierre de derrota usa el rojo y el mensaje "¡Se terminó el juego! ¡Intentalo otra vez!". Ambos ofrecen volver a jugar y volver al menú.
- **FR-016**: Volver a jugar reinicia vidas y selecciones y arma un desafío distinto cuando el catálogo tiene otra combinación posible.
- **FR-017**: Las voces se preparan una sola vez, con la voz Micaela ya usada por el servidor de la PC, y viajan dentro de la aplicación. Incluyen todas las pistas, la pregunta de cada ejercicio de Contar, una bienvenida por juego, la opción elegida, reacciones variables de acierto y de error, el cierre de victoria y de derrota, y reacciones variables para volver a jugar, volver al menú y reelegir una opción ya descartada.
- **FR-018**: Las reacciones variables de acierto, error, volver a jugar, volver al menú y opción ya descartada tienen al menos tres formulaciones distintas cada una.
- **FR-019**: La versión publicada, al pedir el nombre, dice "Hola!, soy Teo, cuál es tu nombre?". La versión de un solo archivo pide el nombre en pantalla y no depende de esa frase ni de una conexión.
- **FR-020**: Las dos entregas salen del mismo origen y conservan las mismas reglas, los mismos datos y los mismos recursos. Una es un sitio estático publicable por HTTPS, también dentro de una carpeta de un sitio. La otra es el archivo único AppParaExpo-Offline.html, autónomo, que se abre desde el almacenamiento sin internet, sin instalación y sin descargar recursos.
- **FR-021**: La versión publicada anota, al terminar cada partida, una fila de planilla de Google Sheets con nombre, juego, si ganó o perdió, y fecha y hora. La credencial de esa planilla es un dato de configuración y no se muestra en la interfaz. Junto con la aplicación se entrega un archivo de texto con el programa de la planilla para recibir esas filas.
- **FR-022**: La versión de un solo archivo no registra ni envía estadísticas. Un fallo o la ausencia de la planilla no impide terminar la partida publicada.
- **FR-023**: La documentación de AppParaExpo explica por separado cómo preparar la versión publicada, cómo publicarla, cómo usar el archivo sin internet, cómo copiarlo a una tablet y en qué navegadores se puede abrir.
- **FR-024**: El estado y los errores de un juego no cambian la partida de otro juego.

### Key Entities

- **Visita**: Nombre del niño durante esa apertura de la aplicación. No es una cuenta. Identifica las filas de la versión publicada.
- **Ficha de sombra**: Un elemento del catálogo de sombras, con identificador, silueta, nombre visible tomado de su primera respuesta, otras formas de decirlo que no se muestran como tarjetas, y sus pistas. El catálogo actual tiene 54 fichas y dos pistas por ficha.
- **Pareja de Sombras**: Una silueta y su nombre dentro de la partida. Puede estar libre, seleccionada o resuelta.
- **Ficha de adivinanza**: Un objeto del catálogo, con identificador, dibujo, primera respuesta válida usada como opción correcta, otras formas de decirlo que no generan botones extra, y tres pistas. El catálogo actual tiene 67 objetos.
- **Ronda de adivinanza**: Un objeto secreto, las pistas ya reveladas y cinco opciones, de las cuales cuatro son distractores de otros objetos.
- **Ejercicio de contar**: Una imagen, su pregunta y su cantidad correcta, tomada de la primera respuesta. El catálogo actual tiene las cantidades 1 a 10.
- **Partida**: El juego elegido, el desafío sorteado, las vidas restantes, las opciones ya resueltas o descartadas y el resultado final, cuando existe.
- **Cierre**: El aviso compartido de victoria o derrota y las dos acciones para seguir.
- **Frase hablada**: Un texto fijo del catálogo o una variante de reacción, asociado a una grabación incluida en la aplicación.
- **Resultado de visita**: Una fila producida solo por la versión publicada, con nombre, juego, resultado y momento en que terminó la partida.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un adulto puede escribir el nombre y abrir cualquiera de los tres juegos en menos de 30 segundos, sin instrucción adicional.
- **SC-002**: En una tablet horizontal de 1280 por 800, el menú y cada juego muestran sus acciones principales sin desplazamiento de la página.
- **SC-003**: En 360 por 640, 800 por 1280, 1280 por 800 y 1366 por 768, los controles de la partida y del cierre se pueden tocar y el texto principal se puede leer.
- **SC-004**: Cada partida de Sombras ofrece cinco parejas reales del catálogo, con nombres mezclados, y se puede completar tanto arrastrando como con dos toques.
- **SC-005**: Un error muestra su consecuencia visual en menos de un segundo: línea que se rompe o opción que se apaga, corazón correspondiente y, cuando corresponde, la pista nueva.
- **SC-006**: Las tres vidas se consumen solo con errores nuevos. Al acertar el objetivo, el cierre verde aparece en ese momento. Al tercer error, el cierre rojo aparece en ese momento.
- **SC-007**: Volver a jugar deja las tres vidas restauradas y presenta otro desafío disponible. Volver al menú deja el juego anterior sin selecciones ni errores.
- **SC-008**: Las imágenes usadas por las partidas, incluida la silueta existente del delfín, se ven en las dos entregas. No quedan referencias a archivos ausentes.
- **SC-009**: Con la red desactivada, el archivo único se abre desde el almacenamiento, muestra el menú y permite jugar, acertar, fallar, repetir y volver al menú en los tres juegos, con sus imágenes y sus voces incluidas.
- **SC-010**: La versión publicada y el archivo único coinciden en reglas, catálogos, dibujos y frases. La preparación de la versión publicada termina correctamente y queda lista para copiarse a un sitio estático.
- **SC-011**: Cada partida terminada en la versión publicada configurada agrega una sola fila con los cuatro datos pedidos. El archivo único agrega cero filas.
- **SC-012**: Después de crear la aplicación, el contenido de ProyectoParaRasperrypiV5 permanece igual.
- **SC-013**: Al repetir cinco veces un acierto, un error, volver a jugar y volver al menú, la voz no usa una única formulación en ninguna de esas cuatro situaciones.

## Assumptions

- La mención a cuatro siluetas en la descripción del tablero queda resuelta como cinco. La apertura del juego, la selección de la partida y la condición de victoria piden cinco parejas, y un tablero de cuatro no podría cumplirse.
- El nombre visible de una sombra y la opción correcta de una adivinanza son la primera respuesta de su ficha. Las respuestas siguientes no se muestran como otras tarjetas.
- El orden de pistas primera, tercera y segunda se aplica por silueta en Sombras y por ronda en Adivina. Si la pista indicada no existe, se toma la siguiente de ese orden que sí exista.
- Al cerrar Adivina, ganado o perdido, se puede mostrar el dibujo y el nombre del objeto. Durante la partida el dibujo sigue oculto.
- Las frases variables se redactan en español rioplatense, con al menos tres variantes por situación. Los mensajes de cierre y el aviso de pista usan exactamente los textos pedidos.
- La bienvenida nombra el juego al que se entra. En Contar, la pregunta de la ficha cumple el lugar de la pista hablada.
- El nombre dura hasta recargar la página. No hace falta un botón de cuentas. Un nombre admite hasta 40 caracteres visibles.
- Abandonar una partida no genera fila. La fecha y hora de la fila son las del momento en que la partida termina en el dispositivo.
- La credencial de la planilla se carga por configuración en la versión publicada. Si falta, se juega igual.
- No hay una aplicación web previa de estos juegos para reutilizar. La aplicación nueva se prepara como sitio estático, con la organización que el pedido ya fijó, y las dos entregas se generan desde ese mismo origen.
- Esta aplicación queda fuera de la base Android del piloto de TEO. Consulta el catálogo, los dibujos y la voz ya elegida del proyecto existente, y no cambia la app, el peluche ni el servidor.
- El logo es el logo oficial ya existente de TEO, copiado dentro de AppParaExpo. Los dibujos de los juegos salen de la carpeta de medios original. No se diseñan reemplazos.
- La comprobación principal del archivo sin internet se hace abriéndolo en Chrome o Edge de escritorio, con la red desactivada, y el mismo archivo es el que se copia a la tablet. La versión publicada es la vía prevista cuando un navegador no permite abrir un archivo local con interacción.
- Quedan fuera Piedra, papel o tijera, música, cuentos, un servidor propio de la exposición y cualquier edición del proyecto original.
