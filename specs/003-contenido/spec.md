# Feature Specification: Contenido, música y cuentos

**Feature Branch**: `001-inicio-base-visual`

**Created**: 2026-10-14

**Status**: Draft

**Input**: User description: "Módulo completo de Contenido —Música y Cuentos— en la app principal y en la vista previa, según los bocetos elegidos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Elegir música o cuentos (Priority: P1)

Quien acompaña a TEO abre Contenido y elige entre Música y Cuentos sin perder el encabezado ni la navegación inferior. Al volver, permanece en la pestaña que estaba usando. Cambiar de pestaña o salir de Contenido no corta lo que TEO está reproduciendo.

**Why this priority**: Sin esta estructura no se puede usar el resto del módulo.

**Independent Test**: Abrir Contenido, pasar de Música a Cuentos, ir a Inicio y volver: la pestaña y la reproducción se conservan.

**Acceptance Scenarios**:

1. **Given** Contenido abierto en Música, **When** se elige Cuentos, **Then** cambia el catálogo y el indicador petróleo, y la reproducción sigue.
2. **Given** una pestaña elegida, **When** se sale y se regresa, **Then** se recupera esa pestaña.
3. **Given** un formulario de cuento, **When** está abierto, **Then** la navegación inferior no se muestra y se puede volver o cancelar.

---

### User Story 2 - Administrar y reproducir música (Priority: P1)

La persona agrega un audio con el selector del teléfono, ve el nombre, el formato y el tamaño reales, y reconoce cuál está sonando. Puede reproducir un archivo concreto, detenerlo y eliminarlo con una confirmación que nombra ese archivo.

**Why this priority**: La música es una de las dos funciones del bloque.

**Independent Test**: Con TEO disponible, subir un audio admitido, verlo en el catálogo, reproducirlo, detenerlo y eliminar esa pieza.

**Acceptance Scenarios**:

1. **Given** un audio del formato admitido, **When** se envía, **Then** se ve el nombre y un avance indeterminado, y el archivo entra al catálogo solo después de confirmarse el resultado.
2. **Given** un envío en curso, **When** se intenta otro, **Then** no se duplica.
3. **Given** una canción confirmada como activa, **When** se mira el catálogo, **Then** esa pieza está señalizada y Detener está disponible.
4. **Given** una respuesta que indica que TEO está dormido, **When** se pidió reproducir, **Then** se muestra ese estado y la canción no queda marcada como activa.
5. **Given** la eliminación, **When** se confirma, **Then** la pregunta identifica el archivo.

---

### User Story 3 - Administrar y narrar cuentos (Priority: P1)

La persona ve título y cantidad de palabras, agrega un PDF, lo hace leer, edita título y texto, y elimina con confirmación. Agregar reúne archivo, nombre, tamaño, cambio de archivo, título y ayuda. Editar precarga título y texto.

**Why this priority**: Los cuentos son la otra función del bloque.

**Independent Test**: Agregar un PDF válido, ver palabras, pedir la lectura, editar y cancelar o guardar, y eliminar con confirmación.

**Acceptance Scenarios**:

1. **Given** un PDF con texto seleccionable de hasta 8 MB, **When** se omite el título, **Then** sigue disponible un título automático; si se escribe, no supera 80 caracteres.
2. **Given** un rechazo al publicar o guardar, **When** llega el motivo, **Then** el formulario conserva lo escrito y no se cierra.
3. **Given** la edición, **When** el teclado está abierto, **Then** se puede llegar a Guardar y Cancelar.
4. **Given** una lectura confirmada, **When** se mira Cuentos, **Then** el bloque de lectura muestra ese cuento y Detener corresponde a la narración.

---

### User Story 4 - Entender carga, vacío, fallo y desconexión (Priority: P2)

La persona distingue si el catálogo se está consultando, si no hay archivos, si la consulta falló, si no hay conexión o si TEO está dormido. Un fallo no se presenta como catálogo vacío. Administrar y reproducir dependen de lo que realmente se puede hacer, sin apagar toda la pantalla.

**Why this priority**: Evita decisiones sobre un estado falso.

**Independent Test**: Simular vacío, error y desconexión y comprobar que el mensaje y las acciones no se confunden.

**Acceptance Scenarios**:

1. **Given** una consulta fallida, **When** se muestra Contenido, **Then** no aparece el mensaje de catálogo vacío.
2. **Given** desconexión, **When** hay una reproducción conocida, **Then** Detener no se ofrece como si siguiera vigente.
3. **Given** una acción pendiente, **When** todavía no hay respuesta, **Then** no se afirma que el audio se escuchó.

---

### Edge Cases

- Nombre de archivo o título largo: se envuelve, sin recortar el texto de la persona.
- Archivo de música fuera de los formatos admitidos, o PDF que supera 8 MB o no es PDF: se explica y no se envía.
- HTTP exitoso con rechazo o sueño: se muestra el motivo y no se publica ni se marca la reproducción.
- Varias líneas en el texto del cuento y cambios conservados si el guardado se rechaza.
- La vista previa recorre las mismas pantallas con datos de ejemplo, identificadas como simulación, sin consultar a TEO ni guardar datos reales.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Contenido ofrece las pestañas Música y Cuentos, con indicador petróleo, encabezado compartido y navegación inferior.
- **FR-002**: Agregar y editar un cuento son pantallas completas, con regreso y Cancelar, sin navegación inferior.
- **FR-003**: La pestaña elegida se conserva al volver. Cambiar de pestaña o salir no detiene la reproducción.
- **FR-004**: La música se elige con el selector del teléfono, se valida según los formatos admitidos y se lee fuera del hilo principal.
- **FR-005**: El catálogo de música muestra nombre, formato y tamaño reales. El envío muestra el nombre y un indicador indeterminado, no se duplica y solo incorpora el archivo tras confirmar el resultado.
- **FR-006**: Se puede reproducir un archivo concreto, señalar el activo y detenerlo. La eliminación pide confirmación nombrando el archivo.
- **FR-007**: El catálogo de cuentos muestra título y cantidad de palabras informados por la fuente. Se puede agregar, leer, editar y eliminar.
- **FR-008**: Agregar cuento muestra el PDF elegido, su nombre y tamaño, permite cambiarlo, pide un título de hasta 80 caracteres y conserva la obtención automática si se deja vacío. El PDF admitido tiene texto seleccionable y pesa hasta 8 MB.
- **FR-009**: Editar precarga título y texto. El título es obligatorio y tiene máximo de 80 caracteres. El texto es multilínea y desplazable. Guardar y Cancelar siguen alcanzables con el teclado abierto.
- **FR-010**: Publicar o cerrar el formulario solo ocurre ante una aceptación. Un rechazo muestra el motivo y conserva el contenido.
- **FR-011**: Una canción no se marca activa antes de la respuesta. Se lee el cuerpo y el estado posterior. Si TEO está dormido u otro límite del acuerdo, se muestra ese estado sin afirmar que el audio se oyó.
- **FR-012**: Inicio y Contenido muestran la misma reproducción vigente. Detener permanece visible y solo se habilita cuando hay una reproducción confirmada que corresponde detener, con la operación de música o de cuento según el caso.
- **FR-013**: Se distinguen carga, catálogo vacío, fallo, desconexión, sueño y acción pendiente. Un fallo de consulta no equivale a un catálogo vacío.
- **FR-014**: La vista previa usa las mismas pantallas, con ejemplos aislados, y permite recorrer pestañas, formularios, confirmaciones y los estados de reproducción, subida, vacío, error y desconexión.
- **FR-015**: No se agregan pausa, avance, reanudación desde un punto, búsqueda en línea ni controles de porcentaje o cancelación de subida que no existan.

### Key Entities

- **Pieza musical**: nombre, formato, tamaño y si es la reproducción confirmada.
- **Cuento**: identificador, título, cantidad de palabras y texto narrable.
- **Reproducción vigente**: canción o cuento confirmado, o ninguna.
- **Envío en curso**: nombre del archivo mientras el resultado no está confirmado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En una revisión visual, Música, el envío, Cuentos, el alta y la edición se reconocen frente a sus bocetos sin bloques vacíos grandes ni controles superpuestos.
- **SC-002**: Quien recorre la vista previa puede ver las dos pestañas, ambos formularios, las dos confirmaciones y los cinco estados representativos sin contactar a TEO.
- **SC-003**: Una respuesta rechazada o de sueño no deja una canción marcada como activa ni cierra un formulario con cambios sin guardar.
- **SC-004**: Al volver a Contenido, la pestaña anterior sigue seleccionada y la reproducción que sigue vigente coincide con Inicio.

## Assumptions

- Los formatos de audio admitidos son los que ya acepta el acuerdo actual: MP3, WAV, OGG, M4A y FLAC.
- El título automático al omitirlo y la revisión del cuento siguen las reglas ya existentes del servidor.
- Este bloque no cambia el peluche ni el servidor salvo que una prueba muestre que un campo necesario se pierde.
- Inicio y Rutinas conservan su presentación ya aprobada.
- La comprobación de que el audio se oyó en el peluche físico queda pendiente si el dispositivo no está disponible.
