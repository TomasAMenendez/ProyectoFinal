# Feature Specification: Base visual e Inicio de TEO

**Feature Branch**: `001-inicio-base-visual`. El directorio `specs/001-inicio-base-visual` se mantiene.

**Created**: 2026-10-14

**Status**: Draft

**Input**: User description: "Primer bloque del rediseño de TEO: base visual compartida, pantalla Inicio y dependencias mínimas de navegación. El adulto reconoce conexión y actividad, duerme o despierta a TEO, consulta interacciones y la última señal emocional, inicia música, detiene una reproducción disponible, celebra un logro y abre las secciones relacionadas. La composición sigue el Inicio elegido. Quedan fuera el rediseño del resto de pantallas, los cambios al peluche o al servidor, y las decisiones aplazadas de tiempo de uso, pulsador y agenda sin TEO."

## Clarifications

### Session 2026-10-14

- Q: ¿Cómo se ordenan los avisos cuando la fecha del evento no es válida? → A: Se usa la fecha del evento si es válida. Si no lo es, se usa una sola vez el momento en que la app recibió el aviso, solo para ordenar y sin mostrarlo. La fecha ausente se omite. El mismo criterio ordena el aviso de Inicio y la lista de hasta veinte.
- Q: ¿Qué puede confirmar la app cuando la celebración se envía? → A: Si el cuerpo confirma que la celebración fue enviada, se informa «Celebración enviada a TEO». No se afirma que el festejo terminó. Dormido, desconectado o no disponible se informan como condición, no como festejo realizado.
- Q: ¿Cuándo el globo muestra cero y cuándo muestra ausencia? → A: Muestra el conteo válido que informa la API, incluido cero. Muestra ausencia si la consulta falla o si el campo falta o es inválido. El contrato no permite separar todos los ceros de respaldo de un cero registrado, y este bloque no resuelve esa limitación.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconocer a TEO y cambiar su actividad (Priority: P1)

El adulto abre Inicio y distingue si TEO está conectado y qué actividad tiene. Puede pedirle que duerma o que despierte, y ve el resultado que quedó confirmado. La imagen del personaje no le indica por sí sola si TEO duerme, se prepara o está desconectado.

**Why this priority**: Sin esta lectura, el resto de las acciones cotidianas se interpreta mal. Conexión y actividad son estados distintos.

**Independent Test**: Con TEO conectado y despierto, conectado y dormido, preparándose y desconectado, el adulto identifica la etiqueta correcta y solo encuentra habilitada la acción que corresponde a ese estado.

**Acceptance Scenarios**:

1. **Given** TEO conectado y despierto, **When** el adulto mira la tarjeta de estado, **Then** ve al personaje saludando, el nombre TEO, las indicaciones Conectado y Despierto por texto y no solo por color, y la acción Dormir TEO.
2. **Given** TEO conectado y dormido, **When** el adulto mira la tarjeta, **Then** ve Conectado y Dormido, y la acción disponible es Despertar TEO.
3. **Given** el adulto pidió despertar a TEO y el peluche todavía se está preparando, **When** Inicio se actualiza, **Then** muestra Preparándose y no da la orden por cumplida.
4. **Given** TEO desconectado, **When** el adulto mira la actividad, **Then** no se afirma que sigue despierto ni dormido si no hay una actividad vigente, y las acciones remotas explican que hace falta recuperar la conexión.
5. **Given** el adulto pide dormir o despertar, **When** llega el resultado, **Then** Inicio muestra el estado confirmado o la condición real que impidió el cambio, no un éxito basado solo en haber enviado la solicitud.

---

### User Story 2 - Reconocer el Inicio acordado (Priority: P1)

El adulto encuentra Inicio claro, cálido y en el orden acordado, aunque el teléfono esté en modo oscuro. Reconoce el logo, el personaje que saluda, el globo de interacciones y la celebración con confeti. Las palabras se leen y los controles se tocan.

**Why this priority**: Este bloque existe para comprobar que la base visual puede reproducir el Inicio elegido antes de extenderla.

**Independent Test**: En el estado de referencia, conectado y despierto, sin reproducción y con datos disponibles, una persona puede señalar cada bloque en orden y leer sus textos sin abrir otra pantalla.

**Acceptance Scenarios**:

1. **Given** Inicio con datos de referencia, **When** el adulto recorre la pantalla, **Then** encuentra, en este orden: encabezado con logo, título Inicio, Avisos y Mi TEO; tarjeta de estado; Hoy con TEO; Ahora con TEO; Celebrar un logro; avisos recientes; y la barra Inicio, Rutinas, Contenido y Agenda, con Inicio seleccionado.
2. **Given** el teléfono en modo oscuro, **When** el adulto está en Inicio, **Then** Inicio se mantiene claro. Al abrir una pantalla beta ya existente, esa pantalla conserva su apariencia actual.
3. **Given** la tarjeta de estado, **When** el adulto observa al personaje, **Then** reconoce la pose saludando, con sus proporciones, sin un recuadro opaco que tape la tarjeta blanca y sin las otras skins.
4. **Given** la tarjeta Celebrar un logro, **When** el adulto la lee, **Then** distingue el texto de apoyo del confeti decorativo.

---

### User Story 3 - Consultar el día con TEO (Priority: P2)

El adulto ve cuántas interacciones hay hoy y cuál fue la última señal emocional registrada. Entiende que esa señal es un registro disponible, no una conclusión sobre el estado actual del niño. Puede abrir el detalle de actividad que ya existe.

**Why this priority**: Es el resumen cotidiano de Inicio, después de saber si TEO está disponible.

**Independent Test**: Con un conteo informado por la API, incluido cero, y con una consulta fallida o un campo ausente, el adulto distingue el número de la ausencia y llega a la pantalla actual de actividad.

**Acceptance Scenarios**:

1. **Given** un conteo conocido de interacciones, **When** el adulto mira Hoy con TEO, **Then** ve ese número junto con la palabra interacciones, en un mismo grupo, dentro del globo cian.
2. **Given** que la API informa un conteo válido igual a cero, **When** el adulto mira el globo, **Then** ve cero y no un mensaje de ausencia.
3. **Given** que la consulta falla o que el conteo falta o es inválido, **When** el adulto mira el globo, **Then** ve una ausencia explícita, distinta de cero.
4. **Given** una última señal emocional registrada, **When** el adulto la lee, **Then** la presentación la identifica como último registro disponible.
5. **Given** que no hay señal registrada, **When** el adulto mira ese dato, **Then** ve la ausencia y no una emoción inventada.
6. **Given** Inicio, **When** el adulto elige Ver actividad, **Then** abre la pantalla de actividad que ya existe, sin el aspecto nuevo de este bloque.

---

### User Story 4 - Iniciar música y detener lo que suena (Priority: P2)

El adulto ve si hay música o un cuento en reproducción. Puede iniciar música con el criterio que ya usa la app, detener una reproducción que realmente pueda detenerse y abrir Contenido. Detener sigue visible cuando no hay nada que detener, pero no se puede activar.

**Why this priority**: Reproducir y detener son acciones distintas de dormir y de celebrar, y el boceto las deja en la misma pantalla.

**Independent Test**: Con nada en reproducción, con una canción activa, con un cuento activo y con el catálogo de música vacío, el adulto entiende qué suena y qué puede hacer.

**Acceptance Scenarios**:

1. **Given** que no hay música ni cuento en reproducción, **When** el adulto mira Ahora con TEO, **Then** lee que no hay nada en reproducción, ve Reproducir música habilitado solo si hay una canción disponible, y ve Detener visible y deshabilitado.
2. **Given** una canción en reproducción, **When** el adulto mira el bloque, **Then** ve el nombre de esa canción y Detener habilitado.
3. **Given** un cuento en lectura, **When** el adulto mira el bloque, **Then** ve el título de ese cuento, no el de una canción, y Detener habilitado.
4. **Given** un cuento activo, **When** el adulto usa la acción principal de reproducción, **Then** vuelve a solicitar ese cuento y no se presenta como una reanudación desde un punto guardado ni como el inicio de una canción.
5. **Given** que no hay un cuento activo y hay canciones, **When** el adulto elige Reproducir música, **Then** se usa la última canción elegida si sigue disponible y, si no, la primera del catálogo.
6. **Given** un catálogo de música vacío y nada en reproducción, **When** el adulto intenta reproducir, **Then** entiende que no hay una canción para iniciar y se lo orienta a Contenido, sin anunciar que empezó una reproducción.
7. **Given** una reproducción que puede detenerse, **When** el adulto elige Detener y el estado posterior lo confirma, **Then** Inicio deja de mostrar esa reproducción. Si la solicitud se rechaza, la consulta falla o el estado no la confirma, conserva el estado conocido, no anuncia éxito y explica el problema.
8. **Given** Inicio, **When** el adulto elige Ver contenido, **Then** abre la pantalla actual de archivos, presentada como Contenido, sin rediseñarla.
9. **Given** una reproducción conocida, **When** se pierde la conexión, **Then** esa reproducción deja de presentarse como vigente y no se da por detenida ni por iniciada.

---

### User Story 5 - Celebrar un logro (Priority: P2)

El adulto encuentra la celebración como una acción propia, separada de la música. Al usarla, sabe si la celebración fue enviada a TEO o por qué no pudo enviarse. No recibe un aviso de que el festejo terminó.

**Why this priority**: Celebrar ya existe y el boceto la destaca, pero no debe confundirse con una reproducción ni darse por hecha al enviarla.

**Independent Test**: Con una respuesta de celebración enviada, con TEO dormido y con la función no disponible, el adulto lee tres resultados distintos. Ninguno dice que el festejo terminó.

**Acceptance Scenarios**:

1. **Given** Inicio, **When** el adulto busca la celebración, **Then** encuentra Celebrar un logro, el texto TEO acompaña el festejo y el confeti, visualmente separados de Ahora con TEO.
2. **Given** que el cuerpo de la respuesta confirma que la celebración fue enviada, **When** el adulto termina la acción, **Then** lee «Celebración enviada a TEO» y no lee que el festejo terminó.
3. **Given** TEO dormido o desconectado, o una celebración no disponible, **When** el adulto intenta celebrar, **Then** lee esa condición y la pantalla no anuncia ni el envío ni un festejo terminado.

---

### User Story 6 - Leer los avisos recientes (Priority: P2)

El adulto ve el aviso más reciente en Inicio y puede abrir una lista breve con los avisos que la app ya retuvo. El más reciente se decide por la fecha del evento cuando es válida y, si no lo es, por el momento en que la app recibió el aviso. Cada aviso muestra su tipo y su mensaje. La fecha y la hora del evento aparecen solo cuando son válidas. El momento de recepción no se muestra.

**Why this priority**: Los avisos ya llegan a la app. Este bloque necesita un destino real para la campana y para Ver avisos, sin convertirlos en un archivo histórico.

**Independent Test**: Con un aviso fechado, uno sin fecha válida recibido después, ninguno, una reapertura de la lista y más de veinte recibidos en la sesión, el adulto comprueba el contenido, el orden y la vuelta a Inicio.

**Acceptance Scenarios**:

1. **Given** al menos un aviso retenido, **When** el adulto mira Avisos recientes, **Then** ve el primero según el orden definido, con su tipo y su mensaje, y el acceso Ver avisos.
2. **Given** un aviso con fecha y hora de evento válidas, **When** se muestra en Inicio o en la lista, **Then** esa fecha y hora acompañan al tipo y al mensaje.
3. **Given** un aviso sin fecha de evento válida, **When** se muestra, **Then** aparecen el tipo y el mensaje, y no se inventa una fecha ni se muestra el momento de recepción en su lugar.
4. **Given** avisos con fecha de evento válida y avisos sin ella, **When** el adulto mira el aviso de Inicio o abre la lista, **Then** cada aviso se ordena por su fecha de evento si es válida y, si no lo es, por el momento en que la app lo recibió. Los más nuevos quedan primero. Un aviso sin fecha no queda forzado al final.
5. **Given** una lista ya abierta, **When** el adulto la abre de nuevo sin que haya avisos nuevos, **Then** el orden se mantiene. El momento de recepción no se vuelve a tomar al abrir la pantalla.
6. **Given** la lista, **When** el adulto la recorre, **Then** no encuentra acciones de leído, eliminar o archivar, ni una llamada u otra intervención a partir del aviso.
7. **Given** la lista abierta, **When** el adulto la mira, **Then** ve un título, el regreso a Inicio y no la barra de actividad. La lista sigue clara. **When** elige regresar, **Then** vuelve a Inicio.
8. **Given** que no hay avisos retenidos, **When** el adulto abre la lista, **Then** entiende que no hay avisos disponibles y no se afirma que no ocurrió ningún evento.
9. **Given** avisos ya retenidos y avisos todavía no incorporados, **When** el adulto abre la lista, **Then** ve los retenidos. Abrir la lista no descarta los que todavía no se habían incorporado.

---

### User Story 7 - Abrir las secciones relacionadas (Priority: P3)

Desde Inicio, el adulto llega a Rutinas, Contenido, Agenda, actividad, avisos, Mi TEO y conexión. Ningún acceso del boceto queda mudo. Los destinos que este bloque no rediseña se reconocen como las pantallas actuales.

**Why this priority**: Los accesos forman parte de la composición elegida. Su destino ya está decidido, aunque varias pantallas definitivas lleguen después.

**Independent Test**: Desde Inicio, cada acceso nombrado abre el destino de este bloque y permite volver a un lugar comprensible.

**Acceptance Scenarios**:

1. **Given** Inicio, **When** el adulto elige Rutinas, Contenido o Agenda en la barra, **Then** abre la sección actual correspondiente. Contenido es la pantalla de archivos actual. Agenda es la agenda actual.
2. **Given** Inicio, **When** el adulto elige Mi TEO o Ver conexión, **Then** abre la Configuración actual. Esa llegada es temporal y no presenta Configuración como el diseño final de Mi TEO ni de Conexión.
3. **Given** Inicio, **When** el adulto elige la campana o Ver avisos, **Then** abre la misma lista mínima de avisos recientes.
4. **Given** el encabezado, **When** el adulto lo mira, **Then** no hay un contador de avisos sin leer.

---

### User Story 8 - Usar Inicio en el teléfono real y en una ventana compacta (Priority: P3)

El adulto puede leer y tocar Inicio en el Samsung Galaxy S25 y en una ventana estrecha, también con el texto ampliado. El contenido se desplaza. Las barras del sistema no tapan la barra de navegación ni el encabezado.

**Why this priority**: La fidelidad del boceto no sirve si en el teléfono los controles quedan cortados o ocultos.

**Independent Test**: En el S25 y en una ventana de 360 dp de ancho, con texto normal y ampliado, se recorren todos los bloques y se activan los controles habilitados sin perder etiquetas.

**Acceptance Scenarios**:

1. **Given** Inicio en el Samsung Galaxy S25, **When** el adulto se desplaza, **Then** ve todos los bloques, la barra inferior queda usable sobre el borde del sistema y ningún control queda debajo de las barras.
2. **Given** una ventana de 360 dp de ancho, **When** el texto está ampliado, **Then** Reproducir música y Detener conservan su etiqueta y su área de toque. Si no entran en una fila, pasan a una distribución legible en lugar de ocultarse.
3. **Given** un número grande de interacciones o un aviso largo, **When** el adulto lo lee, **Then** el contenido crece o se desplaza. La letra no se reduce de forma continua para forzarlo dentro de un alto fijo.
4. **Given** el primer uso todavía incompleto, **When** el adulto abre la app, **Then** sigue el recorrido actual de conexión y perfil. Inicio rediseñado no lo saltea.

---

### Edge Cases

- TEO conectado y dormido: hay conexión, la interacción está suspendida y la acción es despertar.
- Despertar pendiente o preparación: se muestra Preparándose y no se anuncia que TEO ya está despierto.
- Desconectado: la actividad no se presenta como vigente. Una reproducción conocida deja de mostrarse como actual. Dormir, despertar, reproducir, detener y celebrar no se dan por ejecutados.
- Carga inicial o acción en curso: el adulto ve que la operación sigue, la acción correspondiente no se puede repetir mientras tanto y no aparece un porcentaje inventado. Al terminar en éxito o en error, los controles vuelven según la disponibilidad real de TEO.
- Error al consultar o al ejecutar: hay un mensaje claro y, cuando corresponde, una forma de reintentar. No se borra el último estado conocido para sustituirlo por un éxito.
- Conteo informado por la API, incluido cero, frente a consulta fallida o campo ausente o inválido. Un cero de respaldo que la API igual envía como número no se distingue de un cero registrado.
- Señal emocional ausente: se muestra la ausencia, no una emoción de ejemplo.
- Catálogo de música vacío y cuento activo: se muestra el cuento y no se pide cargar una canción como si no hubiera nada.
- Catálogo vacío y sin reproducción: no se inicia audio y se orienta a Contenido.
- Aviso número veintiuno en la misma sesión: se conservan los veinte más recientes según el orden definido y se deja fuera el más antiguo según ese mismo orden.
- Aviso sin fecha de evento válida: se ordena por el momento en que la app lo recibió, registrado una sola vez. Esa hora no se muestra y no se fabrica una fecha de evento. No queda obligado a ir al final.
- Texto ampliado y ventana estrecha: los botones de música no se recortan ni se eliminan.
- Modo oscuro del teléfono: Inicio y la lista nueva de avisos siguen claros. Las pantallas beta ya existentes no cambian su tema por este bloque.
- Ojos abiertos del personaje de frente o saludando: no se usan como indicación de despierto, dormido o desconectado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Inicio MUST mostrar, en este orden, encabezado, estado de TEO, Hoy con TEO, Ahora con TEO, Celebrar un logro, avisos recientes y la navegación inferior.
- **FR-002**: El encabezado MUST incluir el logo original, el título Inicio, el acceso Avisos y el acceso Mi TEO, sin contador de no leídos.
- **FR-003**: La tarjeta de estado MUST mostrar el personaje en pose saludando, el nombre TEO, la conexión y la actividad como indicaciones separadas, la acción de dormir o despertar, y Ver conexión.
- **FR-004**: La pose saludando MUST ser la imagen acordada para Inicio y MUST presentarse con el encuadre de su contenido visible, conservando las proporciones y sin recortar el archivo original. El lienzo completo MUST NOT definir ese tamaño. La imagen de frente MUST permanecer disponible como variante y MUST NOT usarse para representar sueño o desconexión. Ninguna de las dos MUST determinar el estado del peluche.
- **FR-005**: Conexión y actividad MUST comunicarse con texto. El color MAY acompañar, pero MUST NOT ser el único medio.
- **FR-006**: Con TEO conectado y despierto, la acción principal MUST ser Dormir TEO. Con TEO conectado y dormido, MUST ser Despertar TEO.
- **FR-007**: Un despertar pendiente o una preparación en curso MUST mostrarse como Preparándose y MUST NOT presentarse como despierto.
- **FR-008**: Sin conexión, Inicio MUST NOT afirmar una actividad vigente de despierto o dormido. Las acciones que dependen del peluche MUST explicar la falta de conexión.
- **FR-009**: Dormir, despertar, reproducir, detener y celebrar MUST mostrar el resultado confirmado por la información disponible. Aceptar la solicitud MUST NOT bastar para anunciar que TEO ejecutó la acción. Reproducir y detener MUST leer el cuerpo disponible y confirmarse con el estado posterior. Si la solicitud se rechaza, la consulta falla o ese estado no confirma la acción, la app MUST NOT anunciar éxito ni borrar el estado conocido, y MUST explicar el resultado. Si se pierde la conexión, ese estado conocido MUST dejar de presentarse como vigente.
- **FR-010**: Hoy con TEO MUST mostrar el número de interacciones y la palabra interacciones como un solo grupo. Un conteo válido informado por la API, incluido cero, MUST mostrarse como ese número. Si la consulta falla o el campo falta o es inválido, MUST mostrarse ausencia. Este bloque MUST NOT prometer distinguir un cero de respaldo de un cero registrado cuando la API envía un número.
- **FR-011**: La señal emocional MUST presentarse como la última registrada. Si no hay registro, MUST mostrarse la ausencia y MUST NOT inventarse una señal.
- **FR-012**: Ver actividad MUST abrir la pantalla de actividad actual, sin rediseñarla en este bloque.
- **FR-013**: Ahora con TEO MUST mostrar el título del cuento activo cuando hay un cuento en lectura, el nombre de la canción activa cuando hay música, o que no hay nada en reproducción cuando no hay ninguna de las dos.
- **FR-014**: Si no hay un cuento activo, Reproducir música MUST usar la última canción elegida que siga disponible y, en su defecto, la primera del catálogo.
- **FR-015**: Si hay un cuento activo, la acción principal de reproducción MUST volver a solicitar ese cuento. MUST NOT prometer pausa, avance ni reanudación desde un punto guardado.
- **FR-016**: Con el catálogo de música vacío y sin reproducción activa, el adulto MUST entender que no hay una canción para iniciar y MUST recibir una orientación a Contenido.
- **FR-017**: Detener MUST permanecer visible. MUST estar deshabilitado cuando no haya una reproducción que pueda detenerse y MUST habilitarse cuando sí la haya.
- **FR-018**: Detener, cuando está habilitado y el resultado queda confirmado, MUST terminar la reproducción que estaba disponible. Si el resultado no queda confirmado, MUST conservarse el estado conocido y MUST explicarse el problema.
- **FR-019**: Ver contenido MUST abrir la pantalla actual de archivos, presentada como Contenido, sin rediseñarla.
- **FR-020**: Celebrar un logro MUST ser una acción distinguible de la reproducción, con el texto de apoyo TEO acompaña el festejo y con confeti decorativo separado de ese texto.
- **FR-021**: Si el cuerpo confirma que la celebración fue enviada, Inicio MUST informar «Celebración enviada a TEO» y MUST NOT afirmar que el festejo terminó. Si TEO está dormido o desconectado, o la función no está disponible, el adulto MUST ver esa condición y MUST NOT ver el envío ni un festejo anunciado como terminado.
- **FR-022**: Avisos recientes en Inicio MUST mostrar el primer aviso según el orden de FR-027, con tipo y mensaje, más el acceso Ver avisos. Sin avisos, MUST mostrar un estado vacío.
- **FR-023**: La campana y Ver avisos MUST abrir la misma lista mínima, con título y regreso a Inicio, y MUST NOT mostrar además la barra de actividad. El regreso MUST volver a Inicio.
- **FR-024**: Cada aviso MUST mostrar su tipo y su mensaje. La fecha y la hora del evento MUST mostrarse solo cuando sean válidas. Si no lo son, MUST omitirse. El momento de recepción MUST NOT mostrarse como fecha del evento.
- **FR-025**: Los tipos visibles MUST ser Logro, Pedido, Atención y Aviso, sin corchetes. Atención corresponde al tipo de malestar ya recibido. Un aviso de tipo general MUST poder mostrarse aunque no sea uno de los otros tres.
- **FR-026**: La lista MUST conservar como máximo los veinte avisos más recientes según el orden de FR-027, sin historial permanente.
- **FR-027**: El aviso de Inicio y la lista MUST usar el mismo orden, del más nuevo al más antiguo. La posición de cada aviso MUST salir de la fecha y hora del evento cuando sean válidas. Si no lo son, MUST salir del momento en que la app recibió ese aviso. Ese momento MUST registrarse una sola vez al recibirlo y MUST NOT actualizarse al abrir Inicio o la lista. Un aviso sin fecha de evento MUST NOT quedar forzado al final por carecer de ella.
- **FR-028**: Abrir la lista MUST usar los avisos ya retenidos. MUST NOT provocar un nuevo pedido que descarte avisos todavía no incorporados a esa lista.
- **FR-029**: La lista MUST NOT ofrecer leído, eliminar, archivar, llamar ni otra intervención a partir del mensaje.
- **FR-030**: La lista mínima MUST usar la misma presentación clara de Inicio. MUST NOT exigirse en este bloque el rediseño completo posterior de Avisos.
- **FR-031**: La barra inferior MUST ofrecer exactamente Inicio, Rutinas, Contenido y Agenda. En Inicio, Inicio MUST estar seleccionado.
- **FR-032**: Mi TEO y Ver conexión MUST abrir la Configuración actual. El bloque MUST dejar explícito que ese destino es temporal y no sustituye a las pantallas finales de Mi TEO, Ajustes, Conexión y Diagnóstico.
- **FR-033**: Inicio y la lista nueva de avisos MUST permanecer claros aunque el teléfono esté en modo oscuro. Las pantallas beta ya existentes MUST conservar su tema actual.
- **FR-034**: El modo noche del peluche MUST NOT aparecer como control de Inicio ni confundirse con el tema claro de la app.
- **FR-035**: Inicio MUST NOT mostrar cifras ni controles de tiempo de uso, ni el ajuste del pulsador, ni presentar como resuelto el acceso a la agenda durante una desconexión.
- **FR-036**: El primer uso y el perfil MUST conservar su recorrido actual. Inicio rediseñado MUST NOT omitir una conexión o un perfil que ese recorrido todavía exige.
- **FR-037**: Los textos MUST estar en español rioplatense. Los títulos y botones MUST NOT convertirse automáticamente a mayúsculas.
- **FR-038**: Los controles MUST poder tocarse con comodidad. El texto MUST crecer con la escala de letra del teléfono. El contenido MUST desplazarse cuando no entra. El teclado, si aparece, MUST NOT ocultar el campo activo ni la acción para terminar.
- **FR-039**: En el Samsung Galaxy S25 y en una ventana de 360 dp de ancho, con texto normal y ampliado, todos los bloques y controles de Inicio MUST seguir siendo legibles y alcanzables, sin quedar bajo las barras del sistema.
- **FR-040**: La comparación visual MUST usar el Inicio elegido como composición y la guía visual común como criterio de componentes. Los números y nombres de ejemplo del boceto MUST NOT quedar fijos como datos de la app.
- **FR-041**: Logo, personaje y confeti MUST revisarse sobre la tarjeta y el fondo reales antes de aceptar la fidelidad visual. Un sustituto temporal MUST NOT contar como revisión terminada.
- **FR-042**: Mientras una acción se está enviando, esa acción MUST quedar temporalmente deshabilitada para evitar una repetición. La espera MUST NOT mostrar un porcentaje que la app no conozca. Al terminar en éxito o en error, los controles MUST recuperarse según la disponibilidad real de TEO.

### Key Entities *(include if feature involves data)*

- **Estado de TEO**: Conexión vigente o no, y actividad vigente entre despierto, dormido, preparándose o no disponible. La actividad no se deduce de la conexión ni de la imagen.
- **Resumen de hoy**: Conteo de interacciones, que puede ser un número conocido o una ausencia, y última señal emocional registrada, que también puede faltar.
- **Reproducción actual**: Nada, una canción identificada o un cuento identificado. Determina el texto de Ahora con TEO y si Detener puede usarse.
- **Aviso reciente**: Tipo, mensaje y, solo si es válida, fecha y hora del evento. También tiene un momento de recepción, fijado una sola vez al recibirlo, que sirve para ordenar cuando la fecha del evento no es válida y no se muestra. La lista retiene hasta veinte según ese orden. No tiene estado de leído ni archivo.
- **Resultado de acción**: Lo que quedó confirmado después de dormir, despertar, reproducir o detener, o la condición real que lo impidió. En celebrar, el resultado posible es celebración enviada, dormido, desconectado o no disponible. No existe un resultado de festejo terminado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el estado de referencia, un revisor identifica los siete bloques en el orden definido sin abrir otra pantalla: encabezado, estado, Hoy con TEO, Ahora con TEO, celebración, avisos recientes y navegación.
- **SC-002**: Los ocho estados definidos tienen una comprobación observable: conectado y despierto, conectado y dormido, preparándose, desconectado, sin datos, reproducción activa, carga y error. En cada uno, la etiqueta y las acciones habilitadas coinciden con esta especificación.
- **SC-003**: Con nada para detener, Detener se ve y no puede activarse. Con música o cuento activo, Detener se ve y puede usarse. El resultado mostrado coincide con lo confirmado después de la acción.
- **SC-004**: Un conteo numérico informado por la API, incluido cero, se muestra como número. Una consulta fallida o un campo ausente o inválido se muestra como ausencia. La pantalla no separa un cero de respaldo de un cero registrado. Una señal emocional presente y una ausente se distinguen.
- **SC-005**: De los avisos retenidos, todos muestran tipo y mensaje. Solo los que tienen fecha y hora de evento válidas muestran ese dato. El aviso de Inicio y la lista siguen el mismo orden: fecha del evento si es válida y, si no, el momento de recepción registrado una sola vez. Reabrir la lista sin avisos nuevos no cambia ese orden. La lista visible no supera veinte y no ofrece leído, eliminar ni archivar.
- **SC-006**: Cada acceso de Inicio abre el destino definido: actividad actual, Contenido actual, Configuración actual para Mi TEO y Ver conexión, lista mínima para la campana y Ver avisos, y las secciones actuales de Rutinas y Agenda.
- **SC-007**: En una captura del Samsung Galaxy S25 y en una ventana de 360 dp con texto ampliado, todos los controles de Inicio se leen y se alcanzan desplazando la pantalla, y ninguno queda cubierto por las barras del sistema.
- **SC-008**: Con el teléfono en modo oscuro, Inicio y la lista nueva de avisos siguen claros. Una pantalla beta abierta desde Inicio conserva la apariencia que ya tenía.
- **SC-009**: Logo, pose saludando y confeti se reconocen en la revisión visual. El personaje no queda reemplazado por otra skin ni encerrado en un fondo opaco que tape la tarjeta. El confeti no sustituye al texto.
- **SC-010**: El informe de cierre enumera qué escenarios se comprobaron, en qué dispositivo o ventana, y qué quedó sin comprobar. La compilación, por sí sola, no alcanza para marcar el bloque como terminado.

## Assumptions

- El adulto responsable es quien usa la app. Hay un solo perfil de niño ya previsto por el recorrido actual.
- La composición de referencia es el Inicio elegido del 13 de octubre de 2026. La guía visual común manda en componentes, colores y medidas de partida. Si una captura real obliga a ajustar una medida, se registra el ajuste sin cambiar el orden ni las funciones.
- Inicio muestra un aviso, el primero según el orden de FR-027, como en el boceto. El resto de los avisos retenidos se consulta en la lista mínima, con ese mismo orden.
- La fecha del evento válida tiene prioridad para ordenar. El momento de recepción solo ocupa su lugar cuando esa fecha no es válida, se fija al recibir el aviso y no se presenta en pantalla. Ya no se aplica la regla que enviaba siempre al final a los avisos sin fecha.
- La etiqueta de la acción principal de reproducción coincide con lo que va a hacer: iniciar música o volver a solicitar el cuento activo. En reposo, sin cuento activo, la etiqueta es Reproducir música.
- La orientación por catálogo vacío nombra Contenido, que es el nombre visible de la pantalla de archivos durante este piloto.
- La lista mínima de avisos es parte de este bloque porque la campana y Ver avisos necesitan un destino. Su aspecto completo queda para el bloque posterior de Avisos. No se copian de ese boceto los colores por tipo ni otras decisiones visuales todavía no elegidas como referencia principal.
- Las pantallas actuales de actividad, archivos, rutinas, agenda y configuración se reutilizan con su comportamiento presente. Este bloque cambia el nombre visible Archivos por Contenido en la navegación de Inicio y en la orientación asociada.
- El personaje de Inicio usa la imagen saludando ya entregada, sin modificar ese archivo. El tamaño visible sale del contenido opaco, no del lienzo con márgenes transparentes, y conserva las proporciones. La imagen de frente queda disponible y no se muestra como estado dormido. La foto grupal con skins permanece como antecedente.
- Si falta el archivo de telemetría de hoy, el servidor puede responder con la telemetría que tiene en memoria. La app muestra ese conteo cuando es un número válido. No distingue todos los ceros de respaldo de un cero registrado, y este bloque no agrega un contrato para hacerlo.
- Las nueve skins siguen pudiendo elegirse en el perfil. Este bloque no rediseña esa pantalla.
- No se fija un porcentaje de parecido visual ni un tiempo máximo de respuesta. La aceptación es la presencia, el orden, la legibilidad y el comportamiento descritos en los criterios de éxito.
- Antes de comparar en el Samsung Galaxy S25 se registran la versión de Android, el tamaño lógico disponible, la escala de fuente, la escala de pantalla y si la navegación es por gestos o por botones. El modelo del teléfono no define solo el espacio útil.

## Out of Scope

- Rediseñar Rutinas, Contenido, Agenda, actividad, Mi TEO, Ajustes, Conexión, Diagnóstico, primer uso, perfil o el resto de las vistas.
- Cambiar el peluche, el servidor o los contratos que la app ya usa.
- Definir o modificar el tiempo de uso, el despertar por pulsador y el uso de la agenda mientras TEO está desconectado.
- Agregar historial persistente de avisos, estados de leído o acciones para responder un aviso.
- Agregar pausa, avance, barra de duración, batería, cámara en vivo, cuenta de usuario, compra de skins o controles de juegos y yoga.
