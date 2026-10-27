# Tasks: Piedra, papel o tijera en la exposición

**Input**: Design documents from `/specs/010-piedra-papel-tijera/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidas. `spec.md` y `quickstart.md` piden Vitest para las nueve reglas, la estabilidad, la cuenta y el azar de la computadora. Esas pruebas se escriben primero y deben fallar antes de la implementación. La cámara real queda para la comprobación manual.

**Organization**: Tareas agrupadas por historia de usuario para poder implementarlas y probarlas por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede avanzar en paralelo (otro archivo, sin depender de una tarea incompleta)
- **[Story]**: Historia a la que pertenece (US1–US4)
- Las fases de preparación, base y cierre no llevan etiqueta de historia

## Path Conventions

- Proyecto único: `AppParaExpo/` en la raíz del repositorio, junto a `ProyectoParaRasperrypiV5`
- No se modifican `ProyectoParaRasperrypiV5/`, `ServidorDePC/` ni el piloto Android
- No se ejecuta `npm test` ni `scripts/copiar-recursos.mjs`: pisarían imágenes más nuevas

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dejar el reconocedor y las tres figuras dentro de la aplicación que ya existe.

- [x] T001 Instalar `@mediapipe/tasks-vision` y registrarlo en `AppParaExpo/package.json` y `AppParaExpo/package-lock.json`
- [x] T002 [P] Copiar sin editar `ProyectoParaRasperrypiV5/juegos/media/Imagen_Piedra.png`, `Imagen_Papel.png` e `Imagen_Tijera.png` a `AppParaExpo/src/recursos/imagenes/`
- [x] T003 Copiar la carpeta `wasm` de `@mediapipe/tasks-vision` a `AppParaExpo/reconocimiento/wasm` y guardar una sola vez el modelo oficial float16 en `AppParaExpo/reconocimiento/gesture_recognizer.task`
- [x] T004 Copiar `AppParaExpo/reconocimiento/` hacia `dist/reconocimiento/` o `offline/reconocimiento/` al cerrar la compilación, según el modo, en `AppParaExpo/vite.config.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Identificar el juego y mostrar una pantalla vacía a la que el menú pueda entrar.

**⚠️ CRITICAL**: Ninguna historia empieza antes de cerrar esta fase.

- [x] T005 Agregar `ppt` a `JuegoId` y a `Pantalla` en `AppParaExpo/src/juegos/comun/tipos.ts`
- [x] T006 [P] Declarar piedra, papel y tijera con su imagen y su clase de modelo en `AppParaExpo/src/juegos/ppt/figuras.ts`
- [x] T007 Abrir una pantalla vacía de Piedra, papel o tijera, con vuelta al menú y sin llamar a `registrarResultado`, en `AppParaExpo/src/pantallas/PantallaPpt.tsx` y `AppParaExpo/src/pantallas/Aplicacion.tsx`

**Checkpoint**: El proyecto compila con el cuarto destino y los tres juegos anteriores siguen anotando la planilla.

---

## Phase 3: User Story 1 - Elegir el cuarto juego (Priority: P1) 🎯 MVP

**Goal**: Ver la cuarta tarjeta, entrar al juego y volver, sin pedir la cámara y sin cambiar los otros tres.

**Independent Test**: Con un nombre ingresado, ver cuatro tarjetas, abrir Piedra, papel o tijera, volver al menú y abrir Sombras, Adivina el objeto y Contar.

### Tests for User Story 1

- [x] T008 [P] [US1] Escribir la prueba de que `ppt` no se anota y los otros tres juegos sí en `AppParaExpo/src/juegos/ppt/planilla.test.ts`

### Implementation for User Story 1

- [x] T009 [US1] Excluir `ppt` del registro en `AppParaExpo/src/juegos/ppt/planilla.ts` y usarlo desde `AppParaExpo/src/pantallas/Aplicacion.tsx`
- [x] T010 [US1] Agregar la tarjeta con `Imagen_Tijera.png` y el nombre visible en `AppParaExpo/src/pantallas/PantallaMenu.tsx`
- [x] T011 [P] [US1] Repartir cuatro tarjetas en horizontal y en vertical, con un color propio para la nueva, en `AppParaExpo/src/estilos/menu.css`
- [x] T012 [US1] Mostrar «¡Prepará tu mano!», «Mostrá uno de estos tres gestos para jugar.», las tres figuras, «Activar cámara» y «Continuar sin cámara», sin pedir permiso todavía, en `AppParaExpo/src/pantallas/PantallaPpt.tsx`

**Checkpoint**: Se entra y se sale del juego. Las figuras se ven. Todavía no hay resultado.

---

## Phase 4: User Story 2 - Jugar tocando las figuras (Priority: P1)

**Goal**: Terminar una ronda sin cámara, ver victoria, empate o derrota, y jugar otra vez.

**Independent Test**: Continuar sin cámara, tocar cada figura y comprobar el mensaje contra la jugada de la computadora. `npx vitest run src/juegos/ppt/reglas.test.ts src/juegos/ppt/azar.test.ts` pasa.

### Tests for User Story 2

- [x] T013 [P] [US2] Escribir las nueve parejas de `data-model.md` en `AppParaExpo/src/juegos/ppt/reglas.test.ts`
- [x] T014 [P] [US2] Escribir que la computadora solo se elige después de la figura de la persona y que un generador fijo puede devolver las tres en `AppParaExpo/src/juegos/ppt/azar.test.ts`

### Implementation for User Story 2

- [x] T015 [US2] Implementar victoria, empate y derrota en `AppParaExpo/src/juegos/ppt/reglas.ts`
- [x] T016 [US2] Implementar la elección uniforme inyectable en `AppParaExpo/src/juegos/ppt/azar.ts`
- [x] T017 [US2] Resolver la ronda al tocar una figura, mostrar Vos y Computadora, y limpiar con «Jugar otra vez» en `AppParaExpo/src/pantallas/PantallaPpt.tsx`
- [x] T018 [P] [US2] Pintar victoria en verde, empate en amarillo y derrota en rojo, con las tres figuras siempre visibles, en `AppParaExpo/src/estilos/ppt.css`

**Checkpoint**: Se puede jugar toda una exposición sin encender la cámara.

---

## Phase 5: User Story 3 - Jugar con la mano frente a la cámara (Priority: P1)

**Goal**: Confirmar puño, palma o tijera con estabilidad, contar 3, 2 y 1, y recién ahí jugar contra la computadora.

**Independent Test**: `npx vitest run src/juegos/ppt/estabilidad.test.ts` pasa. En `npm run preview`, un gesto firme llega a la cuenta y uno breve no. Esta última parte necesita una cámara real.

### Tests for User Story 3

- [x] T019 [P] [US3] Escribir umbral 0,7, tramo de cerca de 800 ms, reinicio al cambiar y anulación de la cuenta en `AppParaExpo/src/juegos/ppt/estabilidad.test.ts`

### Implementation for User Story 3

- [x] T020 [US3] Implementar el tramo y la cuenta en `AppParaExpo/src/juegos/ppt/estabilidad.ts`
- [x] T021 [US3] Cargar MediaPipe solo al activar la cámara, una mano, modo video, clases `Closed_Fist`, `Open_Palm` y `Victory`, rutas `./reconocimiento/wasm` y `./reconocimiento/gesture_recognizer.task`, en `AppParaExpo/src/juegos/ppt/reconocedor.ts`
- [x] T022 [US3] Pedir la cámara frontal sin audio, cerca de 640×480 sin mínimo obligatorio, y reflejar solo la vista en `AppParaExpo/src/juegos/ppt/useCamara.ts`
- [x] T023 [US3] Mostrar la guía, los mensajes de mano y de gesto, la cuenta, el bloqueo de la jugada y el corte si se toca una figura durante `AppParaExpo/src/pantallas/PantallaPpt.tsx`
- [x] T024 [P] [US3] Dar el ancho principal a la cámara en horizontal, ponerla arriba en vertical y no tapar las figuras en `AppParaExpo/src/estilos/ppt.css`

**Checkpoint**: Con la cámara aceptada se juega por gesto. Sin cámara, la historia 2 sigue completa.

---

## Phase 6: User Story 4 - Salir sin dejar la cámara encendida (Priority: P2)

**Goal**: Apagar la cámara al salir y poder jugar con las figuras si el permiso o el modelo fallan.

**Independent Test**: Activar la cámara, volver al menú y ver que se apaga. Rechazar el permiso y terminar una ronda con una figura.

### Tests for User Story 4

- [x] T025 [P] [US4] Escribir las frases de permiso rechazado, cámara ausente, cámara ocupada, modelo que no carga y navegador sin capacidad en `AppParaExpo/src/juegos/ppt/mensajes.test.ts`

### Implementation for User Story 4

- [x] T026 [US4] Mostrar esas frases, con reintento y con las tres figuras todavía usables, en `AppParaExpo/src/juegos/ppt/mensajes.ts` y `AppParaExpo/src/pantallas/PantallaPpt.tsx`
- [x] T027 [US4] Detener las pistas, cerrar el reconocedor y cortar el análisis al desmontar o volver al menú en `AppParaExpo/src/juegos/ppt/useCamara.ts` y `AppParaExpo/src/pantallas/PantallaPpt.tsx`

**Checkpoint**: Salir del juego no deja la cámara encendida. Un fallo no bloquea los botones.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Comprobar las dos entregas y dejar escrito cómo se juega.

- [x] T028 [P] Documentar la cuarta tarjeta, la cámara en `npm run preview` y el límite de `file://` en `AppParaExpo/README.md`
- [x] T029 Ejecutar `npx vitest run src/juegos/ppt`, `npm run build` y `npm run build:offline` según `specs/010-piedra-papel-tijera/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empieza de inmediato. T003 depende de T001. T004 depende de T003. T002 no depende de ellas.
- **Foundational (Phase 2)**: Depende de T002 y T005 antes de las historias. Bloquea US1–US4.
- **User Stories**: US1 después de la base. US2 después de US1, porque completa la misma pantalla. US3 después de US2, porque usa el resultado y las reglas. US4 después de US3, porque cierra la cámara que esa historia enciende.
- **Polish**: Después de la historia que se quiera entregar. La cámara de T029 espera US3 y US4.

### User Story Dependencies

- **User Story 1 (P1)**: Después de la base. No depende de otra historia.
- **User Story 2 (P1)**: Después de US1. Se prueba sin cámara y sin US3.
- **User Story 3 (P1)**: Después de US2. El gesto reutiliza el resultado ya visible.
- **User Story 4 (P2)**: Después de US3. No cambia las reglas.

### Within Each User Story

- La prueba se escribe y se ve fallar antes del código que la cumple.
- Las reglas y el azar quedan listos antes de conectarlos a la pantalla.
- La estabilidad queda lista antes del bucle de la cámara.

### Parallel Opportunities

- T002 puede avanzar mientras T001 instala el paquete.
- T006 puede avanzar junto con T005.
- T008 y T011 tocan archivos distintos.
- T013 y T014 pueden escribirse juntas.
- T018 puede avanzar junto con T015 y T016.
- T024 puede avanzar junto con T021 y T022.
- T025 puede escribirse al empezar US4.
- T028 puede avanzar junto con la compilación de T029.

---

## Parallel Example: User Story 2

```text
Tarea: "Escribir las nueve parejas en AppParaExpo/src/juegos/ppt/reglas.test.ts"
Tarea: "Escribir el azar inyectable en AppParaExpo/src/juegos/ppt/azar.test.ts"
```

Después de que esas pruebas fallen:

```text
Tarea: "Implementar reglas.ts"
Tarea: "Implementar azar.ts"
Tarea: "Pintar el resultado en ppt.css"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Cerrar Phase 1 y Phase 2.
2. Cerrar Phase 3.
3. Parar y comprobar las cuatro tarjetas, la entrada y la vuelta al menú.
4. Seguir solo si los tres juegos anteriores abren como antes.

### Incremental Delivery

1. US1 deja el juego en el menú.
2. US2 lo hace jugable sin cámara.
3. US3 agrega la mano.
4. US4 asegura el apagado y los fallos.
5. T029 comprueba reglas, compilación publicada y HTML autónomo.

### Parallel Team Strategy

Con más de una persona, después de la base:

1. Una persona cierra US1 y US2 en `PantallaPpt.tsx`.
2. Otra prepara `estabilidad.ts` y `reconocedor.ts` sin tocar la pantalla hasta que US2 esté cerrada.
3. US4 espera a que la cámara ya esté conectada.

---

## Notes

- Cada tarea incluye una ruta concreta.
- No se crea otra aplicación ni se regeneran las voces.
- El modelo no se descarga al abrir el juego.
- Abrir `offline/AppParaExpo-Offline.html` con doble clic puede no encender la cámara en Chrome. La prueba de la mano es `npm run preview`.
