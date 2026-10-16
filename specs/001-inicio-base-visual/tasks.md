---

description: "Task list for the TEO home visual base"
---

# Tasks: Base visual e Inicio de TEO

**Input**: Design documents from `/specs/001-inicio-base-visual/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Incluidas. El pedido pide pruebas de comportamiento, separadas de la revisión en el teléfono y de las comprobaciones con el peluche.

**Organization**: Tareas agrupadas por historia de usuario. La rama de trabajo es `001-inicio-base-visual`. El directorio `specs/001-inicio-base-visual` se mantiene. T034 y T035 siguen abiertas.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede hacerse en paralelo porque toca archivos distintos y no depende de una tarea incompleta.
- **[Story]**: Historia de `spec.md`. Setup, base, operaciones en curso y cierre no llevan historia.

## Path Conventions

- App: `ProyectoAndroid/app/src/main/`
- Pruebas de unidad: `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/`
- Referencias: `docs/ui-ux/referencias/`
- No se modifica `ProyectoParaRasperrypiV5`.

## Phase 1: Setup

**Purpose**: Dejar una rama propia y una línea de base antes de editar la app.

- [x] T001 Preparar la rama `001-inicio-base-visual` desde `pc-pi-lan-discovery` con `git switch -c`, sin `reset`, `stash`, `clean` ni descarte de cambios locales, antes de editar `ProyectoAndroid/`. El directorio `specs/001-inicio-base-visual` se mantiene.
- [x] T002 Ejecutar `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` en `ProyectoAndroid` y anotar el resultado de las pruebas existentes, sin modificar código de la app

---

## Phase 2: Foundational

**Purpose**: Recursos y encuadre compartidos. Bloquean las historias que dibujan Inicio.

**⚠️ CRITICAL**: No empezar las historias de interfaz hasta cerrar esta fase.

- [x] T003 [P] Agregar colores, espacios y estilos `teo_` desde `docs/ui-ux/teo-ui-tokens.json` en `ProyectoAndroid/app/src/main/res/values/colors.xml`, `ProyectoAndroid/app/src/main/res/values/dimens.xml` y `ProyectoAndroid/app/src/main/res/values/themes.xml`, sin reemplazar la paleta lavanda. Los estilos de botón `teo_` conservan las mayúsculas escritas y no aplican `textAllCaps`.
- [x] T004 [P] Dejar los tokens `teo_` claros en `ProyectoAndroid/app/src/main/res/values-night/themes.xml`, para que Inicio y la lista de avisos no se oscurezcan cuando el teléfono esté en modo oscuro
- [x] T005 [P] Copiar sin recortar `docs/ui-ux/referencias/logo-teo.png`, `docs/ui-ux/referencias/teo-saludando.png` y `docs/ui-ux/referencias/teo.png` a `ProyectoAndroid/app/src/main/res/drawable/`
- [x] T006 [P] Agregar el vector de confeti sin texto, el icono de campana y el icono de nota musical en `ProyectoAndroid/app/src/main/res/drawable/`
- [x] T007 [P] Escribir `TeoCharacterFrameTest` en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/TeoCharacterFrameTest.kt` para el rectángulo visible de `teo-saludando.png` (371,34 a 927,738 dentro de 1365×768) y una escala uniforme que no use el lienzo completo
- [x] T008 Implementar el encuadre por contenido opaco en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/TeoCharacterFrame.kt`, de modo que `fitCenter` no sea el encuadre de Inicio y los PNG originales no se recorten

**Checkpoint**: Los recursos `teo_` y el cálculo de encuadre se prueban sin teléfono. La vista todavía no presenta el personaje: eso es T013.

---

## Phase 3: User Story 1 - Reconocer a TEO y cambiar su actividad (Priority: P1)

**Goal**: Mostrar conexión y actividad reales, y dormir o despertar solo según el estado confirmado.

**Independent Test**: Conectado y despierto, conectado y dormido, preparándose y desconectado producen la etiqueta y la acción que corresponden. `pending_wake` no se muestra como Despierto.

### Tests for User Story 1

- [x] T009 [P] [US1] Escribir las pruebas de estado visible en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/HomeStatusTest.kt` antes de implementar el mapeo

### Implementation for User Story 1

- [x] T010 [US1] Implementar el mapeo de conexión, `pending_wake`, `models_ready` y `power_on` en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/HomeStatus.kt`
- [x] T011 [US1] Confirmar dormir y despertar con el cuerpo de `POST /api/power` en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardViewModel.kt`, sin copiar el valor pedido cuando hay despertar pendiente
- [x] T012 [US1] Mostrar la tarjeta de estado y quitar de Inicio el anillo de tiempo y el pulsador en `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml` y `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`

**Checkpoint**: La historia 1 se verifica con `HomeStatusTest` y la tarjeta de estado.

---

## Phase 4: User Story 2 - Reconocer el Inicio acordado (Priority: P1)

**Goal**: Reproducir el orden, el logo, el personaje encuadrado en la vista, el confeti y el tema claro de Inicio.

**Independent Test**: En el estado de referencia se reconocen los siete bloques. El personaje visible usa el encuadre de `teo-saludando.png` y no queda achicado por el margen transparente. Inicio sigue claro en modo oscuro. Entrar a una pantalla beta restaura su apariencia.

### Implementation for User Story 2

- [x] T013 [US2] Presentar el personaje en la tarjeta de estado con `TeoCharacterFrame` y `teo-saludando.png`, respetando las proporciones y sin recortar los PNG originales, en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt` y `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml`. `fitCenter` no define ese tamaño y `teo.png` no se usa para dormir ni desconectar. Conservar el orden de logo, globo, celebración y confeti separado del texto, y la tarjeta de estado de T012.
- [x] T014 [US2] Aplicar la presentación clara mientras el destino es Inicio, ocultar ahí la barra de actividad y restaurar la apariencia beta al entrar en un destino beta ya existente, en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/MainActivity.kt`. No nombrar `nav_alerts`: ese identificador todavía no existe. Conservar `OnboardingGate` y el recorrido actual de conexión y perfil.
- [x] T015 [P] [US2] Escribir las etiquetas de los bloques de Inicio en `ProyectoAndroid/app/src/main/res/values/strings.xml`, con las mayúsculas del texto y sin depender de una transformación automática

**Checkpoint**: La composición de Inicio puede compararse con el boceto elegido, sin dar por cerrada la revisión del S25. La lista de avisos todavía no forma parte de este tema.

---

## Phase 5: User Story 3 - Consultar el día con TEO (Priority: P2)

**Goal**: Mostrar el conteo válido, incluido cero, y la última señal registrada. Mostrar ausencia cuando el dato no llega.

**Independent Test**: Un número de la API, incluido cero, se distingue de una consulta fallida o un campo ausente o inválido. Ver actividad abre la pantalla actual.

### Tests for User Story 3

- [x] T016 [P] [US3] Escribir las pruebas de conteo y señal en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/TodaySummaryTest.kt`

### Implementation for User Story 3

- [x] T017 [US3] Interpretar `summary.total_interactions` y el último evento emocional sin valor inicial cero ni emoji en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardViewModel.kt`
- [x] T018 [US3] Mostrar el globo y la señal, y abrir `nav_metrics` desde Ver actividad, en `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml` y `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`

**Checkpoint**: La historia 3 no promete separar un cero de respaldo de un cero registrado.

---

## Phase 6: User Story 4 - Iniciar música y detener lo que suena (Priority: P2)

**Goal**: Mostrar canción, cuento o nada en reproducción, confirmar el resultado con el estado posterior y abrir Contenido desde Ver contenido.

**Independent Test**: Sin reproducción, Detener se ve y no se activa. Con cuento activo, la acción dice «Volver a leer cuento». Una solicitud rechazada, un error de consulta o un estado que no confirma no anuncian éxito ni borran lo conocido. La desconexión deja de presentar esa reproducción como vigente.

### Tests for User Story 4

- [x] T019 [P] [US4] Escribir las pruebas de reproducción en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/NowPlayingTest.kt`: etiqueta y habilitación, solicitud rechazada, error de la consulta posterior, estado que no confirma la acción, conservación del estado conocido y reproducción que deja de estar vigente al desconectarse

### Implementation for User Story 4

- [x] T020 [US4] Leer el cuerpo de reproducir y detener y confirmarlo con el estado posterior en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardViewModel.kt`, `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt` y `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml`. Conectar canción, cuento, «Reproducir música», «Volver a leer cuento», Detener y el catálogo vacío. No anunciar éxito ni borrar el estado conocido si la solicitud se rechaza, la consulta falla o el estado no confirma. Si se pierde la conexión, indicar que esa reproducción deja de estar vigente.
- [x] T021 [US4] Al tocar Ver contenido, navegar a `nav_stories` desde `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`. Cambiar además el nombre visible Contenido en `ProyectoAndroid/app/src/main/res/values/strings.xml` y `ProyectoAndroid/app/src/main/res/menu/bottom_navigation.xml`. Esos textos y el menú no completan la tarea sin el evento del fragmento.

**Checkpoint**: La historia 4 se verifica sin peluche mediante `NowPlayingTest`. El toque de Ver contenido queda conectado en el fragmento.

---

## Phase 7: User Story 5 - Celebrar un logro (Priority: P2)

**Goal**: Informar el envío de la celebración solo cuando el cuerpo lo confirma, sin anunciar que el festejo terminó.

**Independent Test**: Celebración enviada, TEO dormido y función no disponible producen tres textos distintos. Ninguno dice que el festejo terminó.

### Tests for User Story 5

- [x] T022 [P] [US5] Escribir las pruebas del resultado de celebración en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/CelebrationResultTest.kt`

### Implementation for User Story 5

- [x] T023 [US5] Mostrar «Celebración enviada a TEO» solo ante el cuerpo de envío confirmado, y la condición real en los demás casos, en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt` y `ProyectoAndroid/app/src/main/res/values/strings.xml`

**Checkpoint**: No se agrega un estado ni una ruta de festejo terminado.

---

## Phase 8: Operaciones en curso

**Purpose**: Impedir una segunda solicitud mientras la operación sigue, y devolver los controles al terminar.

**Independent Test**: Durante dormir, despertar, reproducir, detener o celebrar no sale otra solicitud igual. La espera no muestra un porcentaje. Después de éxito o error, los controles coinciden con la disponibilidad real de TEO.

### Tests

- [x] T024 [P] Escribir las pruebas de acción en curso en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/ui/dashboard/HomeActionInProgressTest.kt`: solicitud duplicada impedida, espera sin porcentaje, y controles recuperados después de éxito y de error según la disponibilidad real

### Implementation

- [x] T025 Impedir la repetición de la acción que se está enviando, mostrar la espera sin un porcentaje inventado y recuperar los controles después de éxito o error, según conexión y estado confirmado, en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardViewModel.kt` y `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`

**Checkpoint**: `HomeActionInProgressTest` pasa sin teléfono ni peluche. No se inventa un avance numérico.

---

## Phase 9: User Story 6 - Leer los avisos recientes (Priority: P2)

**Goal**: Conservar tipo, mensaje, fecha del evento y momento de recepción, ordenar con esa regla y mostrar hasta veinte. La migración del tipo y la presentación del aviso destacado son cortes distintos.

**Independent Test**: Un aviso sin fecha válida se ordena por su recepción, registrada una sola vez, y no muestra esa hora. Reabrir la lista no cambia el orden ni vacía la cola. El cambio de `parentAlerts` compila junto con su observador. La lista permanece clara, con título y regreso a Inicio.

### Tests for User Story 6

- [x] T026 [P] [US6] Reescribir las pruebas de orden, tope, etiquetas y fecha omitida en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/network/ParentInboxTest.kt`

### Implementation for User Story 6

- [x] T027 [US6] Cambiar `parentAlerts` a la lista estructurada y actualizar en el mismo corte el observador de `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`, junto con `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/ParentInbox.kt` y `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/network/RobotConnectionManager.kt`. El poll actual sigue siendo el único lector de la cola. Este corte tiene que compilar y no incluye la presentación final del aviso destacado.
- [x] T028 [US6] Presentar el primer aviso retenido en `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml` y `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`. Esta presentación es posterior a la migración de T027.
- [x] T029 [US6] Crear `nav_alerts` y, en el mismo corte, su lista clara con título, regreso a Inicio y barra de actividad oculta, en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/alerts/AlertsFragment.kt`, `ProyectoAndroid/app/src/main/res/layout/fragment_alerts.xml`, `ProyectoAndroid/app/src/main/res/layout/item_parent_notice.xml`, `ProyectoAndroid/app/src/main/res/navigation/mobile_navigation.xml` y `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/MainActivity.kt`. Los textos de esa lista usan los estilos `teo_` y conservan las mayúsculas escritas. Ninguna tarea anterior puede citar `nav_alerts`. Al entrar en un destino beta se restaura la apariencia beta. La lista observa los avisos retenidos y no llama a `GET /api/notifications`.

**Checkpoint**: T027 deja el proyecto compilando. T028 muestra el aviso destacado. T029 es el primer corte que existe `nav_alerts`.

---

## Phase 10: User Story 7 - Abrir las secciones relacionadas (Priority: P3)

**Goal**: Cada acceso de Inicio abre el destino acordado y el primer uso sigue igual.

**Independent Test**: Ver actividad, Ver contenido, Mi TEO, Ver conexión, campana, Ver avisos, Rutinas y Agenda abren los destinos de este piloto. Las pruebas de `OnboardingGate` siguen pasando sin cambios de primer uso.

### Implementation for User Story 7

- [x] T030 [US7] Conectar campana y Ver avisos con `nav_alerts`, Mi TEO y Ver conexión con `nav_config`, y la barra Inicio, Rutinas, Contenido y Agenda, en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`, `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/MainActivity.kt`, `ProyectoAndroid/app/src/main/res/menu/bottom_navigation.xml` y `ProyectoAndroid/app/src/main/res/values/strings.xml`. Conservar `OnboardingGate` y el recorrido actual de conexión y perfil. El toque de Ver contenido sigue siendo el de T021.
- [x] T031 [US7] Ejecutar `OnboardingGateTest` en `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/onboarding/OnboardingGateTest.kt` y no modificar `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/onboarding/OnboardingGate.kt`

**Checkpoint**: Configuración sigue siendo el destino temporal de Mi TEO y Ver conexión. El primer uso no cambia.

---

## Phase 11: User Story 8 - Usar Inicio en el teléfono y en una ventana compacta (Priority: P3)

**Goal**: Mantener legibles y alcanzables los controles con insets, desplazamiento y texto ampliado, sin sumar dos veces el mismo margen.

**Independent Test**: En una revisión de layout, los bloques se desplazan, la barra inferior queda sobre el borde del sistema, los botones de música conservan su etiqueta y el mismo inset no se aplica en el XML y otra vez en código.

### Implementation for User Story 8

- [x] T032 [US8] Aplicar insets, desplazamiento y apilado de Reproducir y Detener en `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/MainActivity.kt`, `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/dashboard/DashboardFragment.kt`, `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/ui/alerts/AlertsFragment.kt`, `ProyectoAndroid/app/src/main/res/layout/fragment_dashboard.xml`, `ProyectoAndroid/app/src/main/res/layout/fragment_alerts.xml` y `ProyectoAndroid/app/src/main/res/layout/content_main.xml`. El encabezado y el desplazamiento reciben el inset superior, y la barra inferior el inferior, una sola vez.

**Checkpoint**: La comprobación en el S25 y a 360 dp queda para el cierre, no para esta tarea de layout.

---

## Phase 12: Polish

**Purpose**: Cerrar las comprobaciones automáticas y dejar separadas las que necesitan teléfono o peluche.

- [x] T033 Ejecutar de nuevo `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` en `ProyectoAndroid` y comparar el resultado con la línea de base de T002
- [ ] T034 Revisar en el Samsung Galaxy S25, en modo oscuro y en una ventana de 360 dp con texto ampliado, el encuadre del personaje, la lista clara de avisos y la composición descritos en `specs/001-inicio-base-visual/quickstart.md`. Anotar esa evidencia. La compilación de T033 no completa esta tarea.
- [ ] T035 Comprobar con el peluche dormir, despertar pendiente, celebración enviada, música y cuento, y anotar ese resultado aparte de las pruebas de unidad, siguiendo `specs/001-inicio-base-visual/quickstart.md`. La compilación de T033 no completa esta tarea.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empieza primero. T002 corre después de T001.
- **Foundational (Phase 2)**: Depende de Setup. Bloquea las historias que dibujan Inicio. T008 espera a T007.
- **User Stories**: US1 empieza después de la base. US2 depende de US1 porque completa `fragment_dashboard.xml` y usa `TeoCharacterFrame`. US3, US4 y US5 dependen de ese layout. Las operaciones en curso esperan las acciones de T011, T020 y T023. US6 espera ese corte antes de volver a editar `DashboardFragment`. US7 espera el destino de avisos. US8 espera los layouts de Inicio y avisos.
- **Polish**: Depende de las historias que se quieran dar por revisadas. T034 y T035 no se sustituyen por T033.

### User Story Dependencies

- **US1 (P1)**: Después de la base. No depende de otra historia.
- **US2 (P1)**: Después de US1 y de T005, T007 y T008. T013 es la integración visible del encuadre.
- **US3 (P2)**: Después de US2 para el globo visible. Su prueba de unidad puede escribirse antes.
- **US4 (P2)**: Después de US2 para el bloque Ahora con TEO. T021 espera el fragmento de T020.
- **US5 (P2)**: Después de US2 para la tarjeta de celebración.
- **Operaciones en curso**: T024 puede escribirse después de la base. T025 espera T011, T020 y T023, y no es paralelo a esas tareas.
- **US6 (P2)**: T026 puede escribirse después de la base. T027 espera T012, T025 y T026, porque comparte `DashboardFragment` con esas tareas y tiene que compilar con el tipo nuevo. T028 espera T027. T029 crea `nav_alerts` y es la primera tarea que puede citarlo. T014 no lo cita.
- **US7 (P3)**: Después de T021 y T029. T030 no reemplaza el evento de Ver contenido.
- **US8 (P3)**: Después de T013 y T029.

### Within Each User Story

- La prueba de comportamiento se escribe antes que la implementación que debe hacerla pasar.
- El mapeo puro se hace antes de conectarlo a la pantalla.
- Las tareas que editan `DashboardFragment.kt` van en orden: T012, T013, T018, T020, T021, T023, T025, T027, T028, T030 y T032.
- No se marca una tarea como hecha al escribirla en este archivo.

### Parallel Opportunities

- T003, T004, T005, T006 y T007 pueden hacerse en paralelo.
- T009, T016, T019, T022, T024 y T026 pueden escribirse en paralelo después de la base, porque están en archivos de prueba distintos.
- T015 puede escribirse junto con T013 y T014 porque toca `strings.xml`.
- T027 no es paralelo a T010, T012 ni T025. Cambiar `parentAlerts` mientras otra tarea edita su observador no es un corte válido.

---

## Parallel Example: pruebas de comportamiento

```text
T009 HomeStatusTest.kt
T016 TodaySummaryTest.kt
T019 NowPlayingTest.kt
T022 CelebrationResultTest.kt
T024 HomeActionInProgressTest.kt
T026 ParentInboxTest.kt
```

Después de que exista la base, pueden escribirse al mismo tiempo esas pruebas. Cada implementación espera la prueba que le corresponde.

---

## Implementation Strategy

### MVP First

1. Completar Phase 1 y Phase 2.
2. Completar US1.
3. Parar y ejecutar `HomeStatusTest`.
4. Completar US2, incluida la presentación del personaje, antes de tratar la composición como revisable.
5. Seguir con US3, US4, US5 y las operaciones en curso.
6. Migrar los avisos en un corte compilable, presentar después el aviso destacado y recién entonces crear `nav_alerts`.
7. Cerrar navegación, adaptación, compilación, captura y peluche en ese orden.

### Incremental Delivery

Cada historia deja un resultado comprobable sin rediseñar Rutinas, Contenido, Agenda, Actividad, Configuración, primer uso ni el servidor. US1 demuestra el estado. US2 demuestra la composición. US6 agrega el único destino nuevo.

---

## Notes

- [P] marca archivos distintos, no permiso para saltear dependencias.
- T034 y T035 siguen abiertas: necesitan el teléfono y el peluche. Una compilación verde no las completa.
- No se crea una API para el festejo terminado ni para distinguir todos los ceros de telemetría.
- T001 crea la rama `001-inicio-base-visual` desde la rama actual y conserva los cambios locales.
- Todas las tareas de implementación de este bloque quedaron hechas. T034 y T035 siguen abiertas.
