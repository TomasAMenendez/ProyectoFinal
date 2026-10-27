# Tasks: Espejito en la exposición

**Input**: Design documents from `/specs/011-espejito/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidas. `spec.md` y `quickstart.md` piden Vitest para la puntuación, el 85 %, el segundo de estabilidad, la calibración y el orden de las cuatro emociones. Esas pruebas se escriben primero y deben fallar antes de la implementación. La cara real queda para la comprobación manual.

**Organization**: Tareas agrupadas por historia de usuario para poder implementarlas y probarlas por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede avanzar en paralelo (otro archivo, sin depender de una tarea incompleta)
- **[Story]**: Historia a la que pertenece (US1–US4)
- Las fases de preparación, base y cierre no llevan etiqueta de historia

## Path Conventions

- Proyecto único: `AppParaExpo/` en la raíz del repositorio, junto a `ProyectoParaRasperrypiV5`
- No se modifican `ProyectoParaRasperrypiV5/`, `ServidorDePC/` ni el piloto Android
- No se ejecuta `npm test` ni `scripts/copiar-recursos.mjs`: pisarían imágenes más nuevas
- No se copia el ranking, los niveles ni las estrellas de `TEO-Juegos.html`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dejar el modelo de la cara junto al reconocedor que la exposición ya usa.

- [x] T001 Extraer una sola vez el modelo embebido en `C:\Users\polol\Downloads\TEO-Juegos.html` y guardarlo en `AppParaExpo/reconocimiento/face_landmarker.task`, sin reemplazar `AppParaExpo/reconocimiento/wasm` ni dejar una URL de descarga en el juego
- [x] T002 [P] Confirmar que el plugin `reconocimiento-local` de `AppParaExpo/vite.config.ts` copia la carpeta completa, incluido `face_landmarker.task`, hacia `dist/reconocimiento/` y `offline/reconocimiento/`; cambiar ese archivo solo si un filtro dejara el modelo afuera

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Identificar el juego y abrir una pantalla a la que el menú pueda entrar, sin encender la cámara y sin anotar la planilla.

**⚠️ CRITICAL**: Ninguna historia empieza antes de cerrar esta fase.

- [x] T003 Agregar `espejito` a `JuegoId` y a `Pantalla` en `AppParaExpo/src/juegos/comun/tipos.ts`
- [x] T004 [P] Declarar feliz, triste, enojado y sorprendido con su imagen, su nombre, su instrucción, su felicitación, sus pesos y su umbral en `AppParaExpo/src/juegos/espejito/emociones.ts`
- [x] T005 Agregar `espejito: 'Espejito'` al mapa de juegos en `AppParaExpo/src/servicios/registro.ts` y la bienvenida en `AppParaExpo/src/audio/textos-fijos.json`, dejando `anotaEnPlanilla` en falso para este juego dentro de `AppParaExpo/src/juegos/ppt/planilla.ts`
- [x] T006 Abrir una pantalla vacía de Espejito, con vuelta al menú, sin `getUserMedia` y sin `registrarResultado`, en `AppParaExpo/src/pantallas/PantallaEspejito.tsx` y `AppParaExpo/src/pantallas/Aplicacion.tsx`

**Checkpoint**: El proyecto compila con el quinto destino. Los cuatro juegos anteriores siguen anotando la planilla. La cámara sigue apagada.

---

## Phase 3: User Story 1 - Elegir Espejito (Priority: P1) 🎯 MVP

**Goal**: Ver la quinta tarjeta, entrar al juego y volver, sin pedir la cámara y sin cambiar los otros cuatro.

**Independent Test**: Con un nombre ingresado, ver cinco tarjetas, abrir Espejito, volver al menú y abrir Sombras, Adivina el objeto, Contar y Piedra, papel o tijera.

### Implementation for User Story 1

- [x] T007 [US1] Agregar la tarjeta ESPEJITO con `Emocion_Feliz.png` en `AppParaExpo/src/pantallas/PantallaMenu.tsx`
- [x] T008 [P] [US1] Repartir cinco tarjetas en horizontal y conservar la columna única en vertical, con el pastel `#9fd0ff` para la nueva, en `AppParaExpo/src/estilos/menu.css`
- [x] T009 [US1] Mostrar el título ESPEJITO, la entrada y el botón Comenzar a jugar, sin pedir permiso todavía, en `AppParaExpo/src/pantallas/PantallaEspejito.tsx`

**Checkpoint**: Se entra y se sale del juego. La tarjeta se ve como las otras. Todavía no hay cámara ni desafíos.

---

## Phase 4: User Story 2 - Imitar las cuatro caras (Priority: P1)

**Goal**: Pedir la cámara al comenzar, calibrar una cara tranquila y confirmar las cuatro emociones, una sola vez y en orden al azar, recién cuando la expresión se sostiene un segundo.

**Independent Test**: `npx vitest run src/juegos/espejito` pasa las puntuaciones, el 85 %, el segundo, la calibración y el sorteo. En `npm run preview`, con una cámara real, cada cara se sostiene y una mueca breve no cuenta. Esa última parte necesita un dispositivo.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T010 [P] [US2] Escribir la sonrisa que supera 0,18, el 0,17 que no alcanza y el caso en que feliz no llega al 85 % de la mayor en `AppParaExpo/src/juegos/espejito/clasificacion.test.ts`
- [x] T011 [P] [US2] Escribir que un paso de 250 ms no completa, que un segundo efectivo sí, y que soltar la expresión hace bajar el avance sin borrarlo de un golpe en `AppParaExpo/src/juegos/espejito/estabilidad.test.ts`
- [x] T012 [P] [US2] Escribir las cinco observaciones en un segundo y medio, los ocho segundos con alguna cara y los ocho segundos sin ninguna cara en `AppParaExpo/src/juegos/espejito/calibracion.test.ts`
- [x] T013 [P] [US2] Escribir que un generador fijo reparte las cuatro emociones una sola vez y que completar la sesión produce un solo `gano` en `AppParaExpo/src/juegos/espejito/sesion.test.ts`

### Implementation for User Story 2

- [x] T014 [P] [US2] Implementar el incremento contra la referencia, los pesos y la regla del umbral más el 85 % en `AppParaExpo/src/juegos/espejito/clasificacion.ts`
- [x] T015 [P] [US2] Implementar el avance con tope de 250 ms, subida en un segundo y bajada en dos en `AppParaExpo/src/juegos/espejito/estabilidad.ts`
- [x] T016 [P] [US2] Implementar la calibración que no abre el desafío si no vio ninguna cara en `AppParaExpo/src/juegos/espejito/calibracion.ts`
- [x] T017 [P] [US2] Implementar el orden inyectable de las cuatro emociones y el cierre de una sesión en `AppParaExpo/src/juegos/espejito/sesion.ts`
- [x] T018 [P] [US2] Crear una sola vez `FaceLandmarker` en modo video, una cara, blendshapes, GPU y luego CPU, con `reconocimiento/wasm/` y `reconocimiento/face_landmarker.task`, en `AppParaExpo/src/juegos/espejito/reconocedor.ts`
- [x] T019 [US2] Pedir la cámara frontal sin audio, cerca de 640×480 sin mínimo, a unas 10 miradas por segundo y con el reflejo solo en la vista, en `AppParaExpo/src/juegos/espejito/useCamara.ts`
- [x] T020 [US2] Encadenar Comenzar a jugar, calibración, desafío, acierto, Siguiente y resumen, deteniendo el análisis de la ronda al confirmar, en `AppParaExpo/src/pantallas/PantallaEspejito.tsx`
- [x] T021 [P] [US2] Poner la imagen grande junto a la cámara en horizontal, mantener el rostro visible en vertical y usar `object-fit: cover` en `AppParaExpo/src/estilos/espejito.css`

**Checkpoint**: Con la cámara aceptada se completan las cuatro caras. Sin un dispositivo, las pruebas automáticas ya cubren la regla. Todavía no hay voces ni fila.

---

## Phase 5: User Story 3 - Entender qué está pasando (Priority: P1)

**Goal**: Decir cuando no se ve la cara o la expresión no coincide, poder repetir la instrucción y apagar la cámara al salir.

**Independent Test**: Tapar la cámara, mostrar otra expresión, pedir repetir y volver al menú. La imagen se apaga y no queda un análisis en marcha.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T022 [P] [US3] Escribir «No te veo todavía. Acercate un poquito a la cámara», «¡Probá imitar la carita!» y los motivos de permiso, ausencia, cámara ocupada, modelo y navegador en `AppParaExpo/src/juegos/espejito/mensajes.test.ts`

### Implementation for User Story 3

- [x] T023 [US3] Implementar esos mensajes, sin ofrecer una partida sin cámara, en `AppParaExpo/src/juegos/espejito/mensajes.ts`
- [x] T024 [US3] Mostrar la ayuda según haya cara o no, el botón Repetir y la pantalla de reintento o salida en `AppParaExpo/src/pantallas/PantallaEspejito.tsx`
- [x] T025 [US3] Detener cada pista con `stop()`, cerrar el detector, cortar el bucle y limpiar las voces al desmontar o volver al menú en `AppParaExpo/src/juegos/espejito/useCamara.ts` y `AppParaExpo/src/pantallas/PantallaEspejito.tsx`

**Checkpoint**: Un fallo de cámara se entiende y se puede salir. El menú no hereda una sesión a medias.

---

## Phase 6: User Story 4 - Voces, planilla y juego sin internet (Priority: P2)

**Goal**: Hablar con la misma voz de la exposición y anotar una sola fila Ganó al completar las cuatro caras, con el modelo ya guardado en el proyecto.

**Independent Test**: Completar una sesión y oír una sola felicitación por emoción. La prueba de registro espera `Espejito` y `Ganó`. Con la red cortada, después de compilar, el juego no pide el modelo afuera.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T026 [P] [US4] Esperar el cuerpo `Juego: Espejito` y `Resultado: Ganó` en `AppParaExpo/src/servicios/registro.test.ts`

### Implementation for User Story 4

- [x] T027 [US4] Hacer que `anotaEnPlanilla` acepte `espejito` en `AppParaExpo/src/juegos/ppt/planilla.ts` y llamar a `onTerminar` una sola vez al abrir el resumen en `AppParaExpo/src/pantallas/PantallaEspejito.tsx`
- [x] T028 [P] [US4] Agregar la bienvenida, la calibración, las cuatro instrucciones, las dos ayudas, las cuatro felicitaciones y el cierre en `AppParaExpo/src/audio/textos-fijos.json`, generar los mp3 que falten con `npm run voces` y dejar el índice en `AppParaExpo/src/recursos/voces/indice.json`
- [x] T029 [US4] Decir cada frase una vez al cambiar de estado, usar la felicitación como aviso de éxito y repetir solo la instrucción actual, mediante `AppParaExpo/src/audio/reproductor.ts` desde `AppParaExpo/src/pantallas/PantallaEspejito.tsx`

**Checkpoint**: Una sesión completa habla y anota una fila. Salir antes no anota. Volver a jugar puede anotar la sesión nueva al completarla.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Comprobar las dos entregas y dejar escrito cómo se juega.

- [x] T030 [P] Documentar la quinta tarjeta, la fila Ganó, la cámara en `npm run preview` y el límite de `file://` en `AppParaExpo/README.md`
- [x] T031 Ejecutar `npx vitest run src/juegos/espejito src/servicios/registro.test.ts src/juegos/ppt/planilla.test.ts`, `npm run build` y `npm run build:offline` según `specs/011-espejito/quickstart.md`, y confirmar que `AppParaExpo/src/juegos/espejito/` no contiene una URL externa del modelo

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empieza de inmediato. T001 y T002 tocan archivos distintos.
- **Foundational (Phase 2)**: Depende de que el destino `espejito` exista en `tipos.ts`. T006 depende de T003. T004 no depende de T003. Bloquea US1–US4.
- **User Stories**: US1 después de la base. US2 después de US1, porque completa la misma pantalla. US3 después de US2, porque habla de la cámara que esa historia enciende. US4 después de US2, porque la fila y las felicitaciones salen de una sesión que ya puede completarse. US3 y US4 se encuentran en `PantallaEspejito.tsx`, así que US4 espera también a US3.
- **Polish**: Después de US4. T031 espera las pruebas y la pantalla cerradas.

### User Story Dependencies

- **User Story 1 (P1)**: Después de la base. No depende de otra historia.
- **User Story 2 (P1)**: Después de US1. La lógica pura se prueba sin cámara y sin US4.
- **User Story 3 (P1)**: Después de US2. No cambia los umbrales ni el orden.
- **User Story 4 (P2)**: Después de US3. No cambia la clasificación.

### Within Each User Story

- La prueba se escribe y se ve fallar antes del código que la cumple.
- `emociones.ts` queda listo antes de la clasificación.
- La clasificación, la estabilidad, la calibración y la sesión quedan listas antes de conectarlas a la pantalla.
- El detector queda listo antes del bucle de la cámara.
- Las frases existen en `textos-fijos.json` antes de reproducirlas.

### Parallel Opportunities

- T002 puede avanzar mientras T001 extrae el modelo.
- T004 puede avanzar junto con T003.
- T008 puede avanzar junto con T007.
- T010, T011, T012 y T013 pueden escribirse juntas.
- T014, T015, T016, T017 y T018 pueden implementarse juntas después de que sus pruebas fallen.
- T021 puede avanzar junto con T018 y T019.
- T022 puede escribirse al empezar US3.
- T026 y T028 pueden avanzar juntas.
- T030 puede avanzar junto con la compilación de T031.

---

## Parallel Example: User Story 2

```text
Tarea: "Escribir clasificacion.test.ts"
Tarea: "Escribir estabilidad.test.ts"
Tarea: "Escribir calibracion.test.ts"
Tarea: "Escribir sesion.test.ts"
```

Después de que esas pruebas fallen:

```text
Tarea: "Implementar clasificacion.ts"
Tarea: "Implementar estabilidad.ts"
Tarea: "Implementar calibracion.ts"
Tarea: "Implementar sesion.ts"
Tarea: "Implementar reconocedor.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Cerrar Phase 1 y Phase 2.
2. Cerrar Phase 3.
3. Parar y comprobar las cinco tarjetas, la entrada y la vuelta al menú.
4. Seguir solo si los cuatro juegos anteriores abren como antes.

### Incremental Delivery

1. US1 deja el juego en el menú, sin cámara.
2. US2 lo hace jugable con la cara y las pruebas automáticas.
3. US3 agrega la ayuda y el apagado.
4. US4 agrega la voz y la fila.
5. T031 comprueba la lógica, la compilación publicada y el HTML autónomo.

### Parallel Team Strategy

Con más de una persona, después de la base:

1. Una persona cierra US1 en el menú y la entrada.
2. Otra prepara `clasificacion.ts`, `estabilidad.ts`, `calibracion.ts` y `sesion.ts` sin tocar la pantalla hasta que US1 esté cerrada.
3. US3 y US4 esperan a que `PantallaEspejito.tsx` ya recorra las cuatro caras, y no se editan al mismo tiempo.

---

## Notes

- Cada tarea incluye una ruta concreta.
- Las cuatro imágenes ya están en `AppParaExpo/src/recursos/imagenes/`. No se redibujan ni se renombran.
- `@mediapipe/tasks-vision` 1.1.1 ya está instalado. No se agrega otro paquete ni un Worker.
- El modelo no se descarga al abrir el juego.
- Abrir `offline/AppParaExpo-Offline.html` con doble clic puede no encender la cámara en Chrome. La prueba de la cara es `npm run preview` con la red cortada después de compilar.
- Los umbrales se mueven solo si una prueba con cara real lo deja anotado. Los textos y la regla del segundo y del 85 % se mantienen.
- No se usa `MarcoJuego`: ese marco muestra corazones y Espejito no tiene vidas.
