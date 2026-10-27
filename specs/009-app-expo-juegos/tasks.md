# Tasks: Juegos de exposición en tablet

**Input**: Design documents from `/specs/009-app-expo-juegos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidas. `plan.md` y `quickstart.md` piden Vitest para las reglas de sorteo, pistas, vidas, victoria, derrota, reinicio y registro. Las pruebas de cada historia se escriben primero y deben fallar antes de la implementación.

**Organization**: Tareas agrupadas por historia de usuario para poder implementarlas y probarlas por separado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede avanzar en paralelo (otro archivo, sin depender de una tarea incompleta)
- **[Story]**: Historia a la que pertenece (US1–US8)
- Las fases de preparación, base y cierre no llevan etiqueta de historia

## Path Conventions

- Proyecto único: `AppParaExpo/` en la raíz del repositorio, junto a `ProyectoParaRasperrypiV5`
- No se modifican `ProyectoParaRasperrypiV5/` ni `ServidorDePC/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Crear la aplicación estática vacía y los dos modos de compilación.

- [x] T001 Crear el proyecto React, TypeScript y Vite en `AppParaExpo/package.json`, `AppParaExpo/tsconfig.json`, `AppParaExpo/index.html`, `AppParaExpo/src/main.tsx` y `AppParaExpo/src/vite-env.d.ts`, con los scripts `dev`, `build`, `preview` y `test`
- [x] T002 Configurar `base: './'`, el modo publicado hacia `AppParaExpo/dist/` y el modo autónomo con `vite-plugin-singlefile` hacia `AppParaExpo/offline/AppParaExpo-Offline.html` en `AppParaExpo/vite.config.ts`
- [x] T003 [P] Documentar la URL y la clave vacías de la planilla en `AppParaExpo/.env.example`
- [x] T004 [P] Excluir dependencias, `dist`, `.env` y cachés locales en `AppParaExpo/.gitignore`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Catálogos, reglas compartidas, estado de la visita y piezas que todos los juegos usan.

**⚠️ CRITICAL**: Ninguna historia empieza antes de cerrar esta fase.

- [x] T005 Agregar la copia de JSON y PNG, corrigiendo en la copia la silueta `delfin` desde `Silueta_Delfín.png`, en `AppParaExpo/scripts/copiar-recursos.mjs` y el script `recursos` de `AppParaExpo/package.json`
- [x] T006 Ejecutar la copia para dejar los catálogos en `AppParaExpo/src/datos/` y los dibujos en `AppParaExpo/src/recursos/imagenes/`
- [x] T007 [P] Definir visita, fichas, partidas y resultado en `AppParaExpo/src/juegos/comun/tipos.ts`
- [x] T008 [P] Establecer el alto visible, las áreas seguras, el tacto sin desplazamiento y la base colorida en `AppParaExpo/src/estilos/base.css`, importada desde `AppParaExpo/src/main.tsx`
- [x] T009 Leer los tres catálogos copiados desde `AppParaExpo/src/datos/catalogos.ts`
- [x] T010 [P] Escribir las pruebas del orden de pistas 1, 3, 2, salteando las ausentes, en `AppParaExpo/src/juegos/comun/pistas.test.ts`
- [x] T011 [P] Escribir las pruebas de tres vidas rotas de derecha a izquierda en `AppParaExpo/src/juegos/comun/vidas.test.ts`
- [x] T012 [P] Escribir las pruebas de cinco sombras únicas, cuatro distractores, contar del 1 al 10 y un desafío distinto al reiniciar en `AppParaExpo/src/juegos/comun/sorteo.test.ts`
- [x] T013 Implementar el orden de pistas en `AppParaExpo/src/juegos/comun/pistas.ts`
- [x] T014 Implementar el descuento de vidas en `AppParaExpo/src/juegos/comun/vidas.ts`
- [x] T015 Implementar el sorteo y la mezcla en `AppParaExpo/src/juegos/comun/sorteo.ts`
- [x] T016 Implementar el nombre, el límite de 40 caracteres y la pantalla activa en `AppParaExpo/src/juegos/comun/visita.ts`
- [x] T017 Reproducir un audio por clave y seguir la jugada si falta el archivo en `AppParaExpo/src/audio/reproductor.ts`
- [x] T018 [P] Crear el control táctil grande en `AppParaExpo/src/componentes/BotonGrande.tsx`
- [x] T019 [P] Crear la representación inicial de tres corazones en `AppParaExpo/src/componentes/Vidas.tsx`
- [x] T020 [P] Crear el panel de cierre reutilizable, todavía sin el acabado final, en `AppParaExpo/src/componentes/Cierre.tsx`
- [x] T021 Cambiar entre nombre, menú y juegos sin rutas del navegador en `AppParaExpo/src/pantallas/Aplicacion.tsx`

**Checkpoint**: Los catálogos cargan, las reglas puras pasan sus pruebas y la aplicación abre una pantalla.

---

## Phase 3: User Story 1 - Entrar a la exposición (Priority: P1) 🎯 MVP

**Goal**: Pedir el nombre, mostrar el menú con el logo de TEO y entrar o salir de cada juego.

**Independent Test**: Escribir un nombre, ver las tres tarjetas, abrir cada destino y volver al menú sin conservar una partida. Comprobarlo en horizontal y en vertical.

### Tests for User Story 1

- [x] T022 [P] [US1] Escribir las pruebas de nombre vacío, nombre recortado y conservación del nombre al cambiar de pantalla en `AppParaExpo/src/juegos/comun/visita.test.ts`

### Implementation for User Story 1

- [x] T023 [P] [US1] Mostrar el logo oficial copiado en `AppParaExpo/src/componentes/LogoTeo.tsx`
- [x] T024 [US1] Pedir y confirmar el nombre antes del menú en `AppParaExpo/src/pantallas/PantallaNombre.tsx`
- [x] T025 [US1] Mostrar las tarjetas SOMBRAS, ADIVINA EL OBJETO y CONTAR en `AppParaExpo/src/pantallas/PantallaMenu.tsx`
- [x] T026 [US1] Distribuir el menú en horizontal y en vertical en `AppParaExpo/src/estilos/menu.css`
- [x] T027 [US1] Abrir un destino con vuelta al menú para los tres juegos en `AppParaExpo/src/pantallas/PantallaSombras.tsx`, `AppParaExpo/src/pantallas/PantallaAdivina.tsx`, `AppParaExpo/src/pantallas/PantallaContar.tsx` y conectarlos en `AppParaExpo/src/pantallas/Aplicacion.tsx`

**Checkpoint**: Se puede recorrer la exposición aunque los tableros todavía no jueguen.

---

## Phase 4: User Story 2 - Jugar a Sombras (Priority: P1)

**Goal**: Unir cinco siluetas con sus nombres arrastrando o con dos toques, y mostrar la pista cuando la unión falla.

**Independent Test**: Resolver una pareja de cada manera, fallar una pareja, ver la pista y ganar al completar las cinco o perder a la tercera falla.

### Tests for User Story 2

- [x] T028 [P] [US2] Escribir las pruebas de pareja correcta, pareja incorrecta, pista por silueta, bloqueo de lo resuelto y victoria o derrota en `AppParaExpo/src/juegos/sombras/partida.test.ts`

### Implementation for User Story 2

- [x] T029 [US2] Implementar el estado de las cinco parejas en `AppParaExpo/src/juegos/sombras/partida.ts`
- [x] T030 [P] [US2] Dibujar la línea temporal y las uniones permanentes en `AppParaExpo/src/juegos/sombras/LineasUnion.tsx`
- [x] T031 [US2] Resolver arrastre y doble toque con un solo manejo de puntero en `AppParaExpo/src/juegos/sombras/useUnion.ts`
- [x] T032 [US2] Mostrar el aviso «No era esa, te doy una pista» y la pista de la silueta en `AppParaExpo/src/componentes/AvisoPista.tsx`
- [x] T033 [US2] Armar el tablero izquierdo/derecho y superior/inferior en `AppParaExpo/src/juegos/sombras/Tablero.tsx`, `AppParaExpo/src/pantallas/PantallaSombras.tsx` y `AppParaExpo/src/estilos/sombras.css`

**Checkpoint**: Sombras se puede ganar y perder con el dedo y con el mouse.

---

## Phase 5: User Story 3 - Adivinar el objeto (Priority: P1)

**Goal**: Elegir el objeto entre cinco nombres, acumular pistas y ocultar el dibujo hasta el final.

**Independent Test**: Fallar dos veces, ver las tres pistas en el orden pedido, acertar en una partida y perder en otra a la tercera vida.

### Tests for User Story 3

- [x] T034 [P] [US3] Escribir las pruebas de opción correcta, distractores, pistas acumuladas, opción apagada y fin de partida en `AppParaExpo/src/juegos/adivina/partida.test.ts`

### Implementation for User Story 3

- [x] T035 [US3] Implementar la ronda y las opciones en `AppParaExpo/src/juegos/adivina/partida.ts`
- [x] T036 [US3] Mostrar pistas y cinco nombres, con el dibujo oculto hasta el resultado, en `AppParaExpo/src/pantallas/PantallaAdivina.tsx` y `AppParaExpo/src/estilos/adivina.css`

**Checkpoint**: Adivina el objeto se puede ganar y perder sin revelar la imagen antes de tiempo.

---

## Phase 6: User Story 4 - Contar (Priority: P1)

**Goal**: Mostrar la imagen y la pregunta del catálogo y elegir la cantidad entre los números del 1 al 10.

**Independent Test**: Ver la pregunta real, fallar un número sin apagar los demás y ganar con la primera respuesta de la ficha, o perder al tercer error.

### Tests for User Story 4

- [x] T037 [P] [US4] Escribir las pruebas de la cantidad del catálogo, del orden 1 al 10 y de los números descartados en `AppParaExpo/src/juegos/contar/partida.test.ts`

### Implementation for User Story 4

- [x] T038 [US4] Implementar la ronda de contar en `AppParaExpo/src/juegos/contar/partida.ts`
- [x] T039 [US4] Mostrar imagen, pregunta y botones del 1 al 10 en `AppParaExpo/src/pantallas/PantallaContar.tsx` y `AppParaExpo/src/estilos/contar.css`

**Checkpoint**: Contar se puede ganar y perder usando solo el número del catálogo.

---

## Phase 7: User Story 5 - Cerrar la partida y volver a jugar (Priority: P1)

**Goal**: Unificar corazones, victoria, derrota, confeti, rejugada y vuelta al menú en los tres juegos.

**Independent Test**: Forzar victoria y derrota en cada juego. Volver a jugar restaura tres vidas y cambia el desafío. Volver al menú borra la partida.

### Tests for User Story 5

- [x] T040 [P] [US5] Escribir las pruebas de reinicio con otro desafío y de limpieza al volver al menú en `AppParaExpo/src/juegos/comun/reinicio.test.ts`

### Implementation for User Story 5

- [x] T041 [US5] Animar la ruptura del corazón derecho, luego el medio y al final el izquierdo en `AppParaExpo/src/componentes/Vidas.tsx` y `AppParaExpo/src/estilos/vidas.css`
- [x] T042 [US5] Mostrar «¡Muy bien! ¡Ganaste!» con confeti, o «¡Se terminó el juego! ¡Intentalo otra vez!», en `AppParaExpo/src/componentes/Cierre.tsx` y `AppParaExpo/src/estilos/cierre.css`
- [x] T043 [US5] Conectar VOLVER A JUGAR y VOLVER AL MENÚ PRINCIPAL con el reinicio y la limpieza en `AppParaExpo/src/pantallas/Aplicacion.tsx`, `AppParaExpo/src/pantallas/PantallaSombras.tsx`, `AppParaExpo/src/pantallas/PantallaAdivina.tsx` y `AppParaExpo/src/pantallas/PantallaContar.tsx`

**Checkpoint**: Los tres juegos terminan con el mismo cierre y pueden empezar de nuevo.

---

## Phase 8: User Story 6 - Escuchar a TEO mientras se juega (Priority: P1)

**Goal**: Incluir las voces de Micaela generadas una sola vez y reproducirlas en cada momento pedido.

**Independent Test**: Oír bienvenida, opción elegida, acierto, error, pista, cierre y las dos salidas. Repetir cinco veces y comprobar que las reacciones variables no usan una sola frase. La apertura hablada solo aparece en la versión publicada.

### Implementation for User Story 6

- [x] T044 [P] [US6] Listar pistas, preguntas, nombres, números y al menos tres variantes por reacción en `AppParaExpo/src/audio/frases.ts`
- [x] T045 [P] [US6] Generar cada texto faltante con `synthesize_wav` de Micaela, sin editar el servidor, y comprimirlo con ffmpeg en `AppParaExpo/scripts/generar-voces.py`
- [x] T046 [US6] Agregar el script `voces` en `AppParaExpo/package.json` y guardar los audios en `AppParaExpo/src/recursos/voces/`
- [x] T047 [US6] Reproducir la apertura, las bienvenidas, las jugadas, las pistas y el cierre desde `AppParaExpo/src/pantallas/PantallaNombre.tsx`, `AppParaExpo/src/pantallas/PantallaSombras.tsx`, `AppParaExpo/src/pantallas/PantallaAdivina.tsx`, `AppParaExpo/src/pantallas/PantallaContar.tsx`, `AppParaExpo/src/componentes/AvisoPista.tsx` y `AppParaExpo/src/componentes/Cierre.tsx`

**Checkpoint**: Las partidas hablan con archivos locales. Una segunda corrida de voces no repite llamadas de textos ya generados.

---

## Phase 9: User Story 7 - Publicar la exposición y también llevarla sin internet (Priority: P1)

**Goal**: Obtener el sitio estático y el HTML autónomo desde el mismo código, sin pedidos externos en el archivo local.

**Independent Test**: `npm run build` termina bien. Con la red desactivada, abrir `AppParaExpo/offline/AppParaExpo-Offline.html` permite jugar los tres juegos, ver imágenes y oír voces. `ProyectoParaRasperrypiV5` permanece igual.

### Implementation for User Story 7

- [x] T048 [US7] Dejar el HTML autónomo autocontenido y con recursos relativos en `AppParaExpo/vite.config.ts`
- [x] T049 [US7] Comprobar que el HTML generado no referencia direcciones externas ni la clave de la planilla en `AppParaExpo/scripts/verificar-offline.mjs`
- [x] T050 [US7] Explicar compilación, publicación, uso sin internet, copia a la tablet y navegadores en `AppParaExpo/README.md`

**Checkpoint**: Las dos entregas juegan con las mismas reglas y el archivo local no descarga nada.

---

## Phase 10: User Story 8 - Anotar el resultado de la visita (Priority: P2)

**Goal**: Agregar una fila al terminar una partida publicada y no enviar nada desde el archivo autónomo.

**Independent Test**: Una victoria y una derrota publicadas producen dos filas con nombre, juego, resultado y fecha y hora. Abandonar el juego no produce fila. El HTML autónomo no envía la solicitud aunque exista configuración.

### Tests for User Story 8

- [x] T051 [P] [US8] Escribir las pruebas del POST al cerrar y de la función vacía en la entrega autónoma en `AppParaExpo/src/servicios/registro.test.ts`

### Implementation for User Story 8

- [x] T052 [P] [US8] Escribir el receptor de filas para pegar en la hoja en `AppParaExpo/planilla-apps-script.txt`
- [x] T053 [US8] Enviar el JSON como `text/plain` solo desde la versión publicada, sin bloquear el cierre, en `AppParaExpo/src/servicios/registro.ts`
- [x] T054 [US8] Disparar el registro una vez, al pasar a ganado o perdido, desde `AppParaExpo/src/pantallas/Aplicacion.tsx`

**Checkpoint**: La planilla recibe partidas terminadas y la exposición sigue si la planilla no responde.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Comprobar el conjunto en los tamaños pedidos y dejar las dos compilaciones verdes.

- [x] T055 Ejecutar `npm test` y corregir las reglas que fallen en `AppParaExpo/src/juegos/`
- [x] T056 Ejecutar `npm run build` y `npm run build:offline`, y revisar el resultado con `AppParaExpo/scripts/verificar-offline.mjs`
- [x] T057 Ajustar el encuadre de 360×640, 800×1280, 1280×800 y 1366×768 en `AppParaExpo/src/estilos/base.css`, `AppParaExpo/src/estilos/menu.css`, `AppParaExpo/src/estilos/sombras.css`, `AppParaExpo/src/estilos/adivina.css` y `AppParaExpo/src/estilos/contar.css`
- [x] T058 Recorrer los escenarios de `specs/009-app-expo-juegos/quickstart.md` en la versión publicada y en el archivo abierto sin red
- [x] T059 Confirmar que `ProyectoParaRasperrypiV5/` y `ServidorDePC/` no tienen cambios después de la copia y de la generación de voces

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: empieza de inmediato.
- **Foundational (Phase 2)**: depende de Setup y bloquea todas las historias.
- **Historias (Phase 3 en adelante)**: dependen de la fase base.
- **Polish (Phase 11)**: depende de las historias que se quieran dar por terminadas. La exposición completa incluye US1 a US8.

### User Story Dependencies

- **US1 (P1)**: empieza después de la base. No depende de otro juego.
- **US2 (P1)**: empieza después de la base. Usa el cierre y las vidas iniciales, sin depender de Adivina ni de Contar.
- **US3 (P1)**: empieza después de la base. Independiente de Sombras y de Contar.
- **US4 (P1)**: empieza después de la base. Independiente de los otros juegos.
- **US5 (P1)**: conviene cerrarla cuando los tres juegos ya muestran una partida, porque conecta los mismos botones en las tres pantallas. La animación de `Vidas.tsx` y `Cierre.tsx` puede adelantarse.
- **US6 (P1)**: depende de las pantallas de US1 a US5 para enganchar cada momento hablado.
- **US7 (P1)**: la configuración empieza en T002. La comprobación del archivo único espera a que juegos, imágenes y voces existan.
- **US8 (P2)**: depende del momento de cierre de US5 y de la separación de entregas de US7.

### Within Each User Story

- La prueba de la historia se escribe y falla antes de implementar su regla.
- El estado de la partida queda antes de la pantalla.
- La pantalla queda antes de integrar el audio o el registro.

### Parallel Opportunities

- T003 y T004 pueden hacerse juntos después de T001.
- T007 y T008 pueden hacerse juntos después de T006.
- T010, T011 y T012 pueden escribirse juntos.
- T018, T019 y T020 pueden hacerse juntos.
- Después de la base, US2, US3 y US4 pueden avanzar a la vez.
- T044 y T045 pueden hacerse juntos.
- T051 y T052 pueden hacerse juntos.

---

## Parallel Example: User Story 2

```text
T028 Pruebas de partida en AppParaExpo/src/juegos/sombras/partida.test.ts
T030 Líneas de unión en AppParaExpo/src/juegos/sombras/LineasUnion.tsx
```

T030 puede empezar junto con la prueba. T031 espera la lógica de T029 y el dibujo de T030.

## Parallel Example: User Stories 2, 3 y 4

```text
US2 AppParaExpo/src/juegos/sombras/ y AppParaExpo/src/pantallas/PantallaSombras.tsx
US3 AppParaExpo/src/juegos/adivina/ y AppParaExpo/src/pantallas/PantallaAdivina.tsx
US4 AppParaExpo/src/juegos/contar/ y AppParaExpo/src/pantallas/PantallaContar.tsx
```

## Parallel Example: User Story 6

```text
T044 Lista de frases en AppParaExpo/src/audio/frases.ts
T045 Generador en AppParaExpo/scripts/generar-voces.py
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 y Phase 2.
2. Completar Phase 3.
3. Parar y comprobar el recorrido de nombre, menú y vuelta.
4. Seguir con Sombras solo si ese recorrido ya se entiende en la tablet horizontal.

### Incremental Delivery

1. Base lista para cargar catálogos reales.
2. US1 entrega la entrada de la exposición.
3. US2 entrega el primer juego completo.
4. US3 y US4 suman los otros dos juegos.
5. US5 unifica el final.
6. US6 agrega la voz guardada.
7. US7 produce las dos entregas.
8. US8 anota la visita publicada.

### Parallel Team Strategy

1. Entre todos se cierran Setup y Foundational.
2. Después, una persona puede tomar Sombras, otra Adivina y otra Contar.
3. El cierre, las voces, el archivo único y la planilla se integran cuando esas pantallas existen.

---

## Notes

- Las tareas [P] tocan archivos distintos y no dependen de otra tarea marcada en el mismo paso.
- Cada historia se puede probar con el criterio indicado en su fase.
- No se inventan fichas ni dibujos. Si falta un recurso, se informa y se sigue con el resto.
- La clave de Cartesia y la clave de la planilla no se escriben en el repositorio ni en el HTML autónomo.
