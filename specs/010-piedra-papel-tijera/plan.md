# Implementation Plan: Piedra, papel o tijera en la exposición

**Branch**: `010-piedra-papel-tijera` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/010-piedra-papel-tijera/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Sumar Piedra, papel o tijera a `AppParaExpo`, sin crear otra aplicación y sin tocar `ProyectoParaRasperrypiV5`, `ServidorDePC` ni el piloto Android. El menú gana una cuarta tarjeta con la tijera original. La partida se resuelve con las reglas clásicas, separadas de la pantalla. La persona puede tocar las tres figuras o mostrar la mano a la cámara frontal. El reconocimiento usa MediaPipe Gesture Recognizer en el dispositivo, con el modelo y el WASM guardados en el proyecto. La planilla no recibe esta partida. El HTML autónomo actual sigue abriendo los juegos por figuras; la cámara se comprueba en la compilación publicada, servida en local, porque un archivo `file://` no puede cargar el reconocedor en Chrome.

## Technical Context

**Language/Version**: TypeScript 5 y React 19, empaquetados con Vite 6. Node.js 20 o superior para instalar y compilar. La tablet solo necesita un navegador.

**Primary Dependencies**: Las que `AppParaExpo` ya usa, más `@mediapipe/tasks-vision`. El WASM sale de ese mismo paquete. El modelo `gesture_recognizer.task` (float16 oficial) se guarda una vez dentro del proyecto. No hay CDN en tiempo de juego. Vitest sigue probando la lógica.

**Storage**: Tres PNG copiados a `AppParaExpo/src/recursos/imagenes/`. El modelo y el WASM viven en `AppParaExpo/reconocimiento/` y se copian a `dist/reconocimiento/` al publicar. La ronda y la cámara quedan en memoria. Esta partida no escribe la planilla.

**Testing**: Vitest para las nueve reglas, el umbral, los 800 ms, el reinicio de la estabilidad y el azar uniforme con un generador inyectado. `npm run build` y `npm run preview` para la cámara. El HTML autónomo se abre con la red cortada para comprobar las figuras. La mano real queda marcada como prueba de dispositivo.

**Target Platform**: La misma exposición: tablet horizontal primero, y también 360×640, 800×1280, 1280×800 y 1366×768. Chrome y Edge actuales. La cámara exige un contexto seguro (`localhost` o HTTPS).

**Project Type**: Ampliación de la aplicación web estática que ya está en `AppParaExpo`.

**Performance Goals**: Como máximo una inferencia a la vez y cerca de 10 por segundo. La interfaz sigue aceptando toques mientras la cámara está abierta. Un gesto firme cerca de 800 ms inicia la cuenta. El resultado aparece antes de 5 s desde el 3.

**Constraints**: No se modifica el peluche ni se vuelven a copiar el resto de sus imágenes. No se piden voces nuevas. No se envían fotogramas. El umbral inicial es 0,7. Una sola mano. La vista en espejo no se aplica al cuadro que clasifica el modelo. El script `npm test` no se usa, porque vuelve a copiar imágenes viejas; las pruebas corren con `npx vitest run`.

**Scale/Scope**: Una tarjeta más, una pantalla y una ronda a la vez. Tres figuras visibles durante toda la partida. Sin registro en planilla y sin frases habladas nuevas.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

La constitución 1.2.0 gobierna el piloto Android. Esta feature solo amplía la exposición web ya exceptuada en `009-app-expo-juegos`.

| Principio | Resultado | Cómo se cumple |
|---|---|---|
| I. Conservar el funcionamiento existente | Pasa | Se copian tres PNG. No se edita el peluche, la app Android ni el servidor. Sombras, Adivina y Contar conservan reglas, voces y planilla. |
| II. Mantener la base técnica | Excepción ya justificada | La exposición sigue en la web de `AppParaExpo`. No se migra el piloto Android. MediaPipe se agrega solo a esa web. |
| III. Respetar las referencias visuales | Excepción ya justificada | No hay boceto de este juego en `docs/ui-ux`. La composición sale de `spec.md` y reutiliza marco, botones y paleta de la exposición. |
| IV. Conservar la identidad acordada | Excepción acotada | El logo y el carácter visual de la exposición se mantienen. No se inventan las tres figuras. |
| V. Mostrar datos y estados reales | Pasa | Victoria, empate y derrota salen de las reglas. El permiso, la ausencia de mano y el fallo del modelo se muestran como tales. No se simula una cámara. |
| VI. Garantizar una interfaz adaptable | Pasa en la web | Cuatro tamaños de `quickstart.md`, toques grandes y las figuras siempre visibles. |
| VII. Separar decisiones pendientes | Pasa | Tiempo de uso, pulsador, agenda y modo noche no entran. |
| VIII. Desarrollar por bloques | Pasa | Este plan cubre solo el cuarto juego. `tasks.md` queda para el comando siguiente. |
| IX. Verificar antes de declarar terminado | Pasa | `quickstart.md` separa reglas automáticas, compilación publicada con cámara, HTML autónomo sin red y la prueba de mano real. |

Revisión posterior al diseño: los contratos no cambian la planilla ni los tres juegos. La cámara se apaga al salir. La limitación de `file://` queda escrita y no se resuelve con un CDN. No aparecieron violaciones nuevas.

## Project Structure

### Documentation (this feature)

```text
specs/010-piedra-papel-tijera/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── recorrido-ppt.md
│   └── reconocimiento.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
AppParaExpo/
├── src/
│   ├── pantallas/
│   │   ├── PantallaMenu.tsx          # cuarta tarjeta
│   │   ├── Aplicacion.tsx            # abre el juego y no lo anota
│   │   └── PantallaPpt.tsx
│   ├── juegos/
│   │   ├── comun/tipos.ts            # JuegoId incluye ppt
│   │   └── ppt/
│   │       ├── reglas.ts
│   │       ├── reglas.test.ts
│   │       ├── estabilidad.ts
│   │       ├── estabilidad.test.ts
│   │       └── azar.ts
│   ├── estilos/
│   │   ├── menu.css                  # cuatro tarjetas
│   │   └── ppt.css
│   └── recursos/
│       └── imagenes/
│           ├── Imagen_Piedra.png
│           ├── Imagen_Papel.png
│           └── Imagen_Tijera.png
├── reconocimiento/
│   ├── wasm/                         # copiado del paquete, no de un CDN
│   └── gesture_recognizer.task
├── scripts/
│   └── copiar-reconocimiento.mjs     # solo estos archivos hacia dist y offline
├── dist/reconocimiento/
└── offline/
    ├── AppParaExpo-Offline.html      # figuras sí; cámara no desde file://
    └── reconocimiento/               # junto al HTML, para un servidor local
```

**Structure Decision**: Se sigue el corte de `AppParaExpo`: la pantalla en `src/pantallas`, la lógica pura en `src/juegos/ppt` y las imágenes en `src/recursos/imagenes`, que es donde el menú ya resuelve los dibujos. No se crea `src/juegos/piedra-papel-tijera/componentes` porque el proyecto no separa componentes por juego. El modelo no entra al grafo del HTML único.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Principio II: el reconocimiento no vive en la app Android | El pedido agrega el juego a la web de exposición y exige cámara en la tablet | Llevarlo al piloto Kotlin no actualiza `AppParaExpo` ni las dos entregas |
| Principio III: no hay boceto en `docs/ui-ux` | La composición de cámara, figuras y resultado ya está cerrada en `spec.md` | Un boceto nuevo reabriría una pantalla que el pedido ya describió |
| Principio IV: la tarjeta nueva usa un color de la exposición, no el marfil de Inicio | El menú actual ya es infantil y colorido | Copiar Inicio rompería las tres tarjetas que la exposición ya muestra |

## Phase 0 y Phase 1

La investigación está en [research.md](research.md). El modelo, los contratos y la guía de comprobación quedaron en [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md). No quedan incógnitas técnicas abiertas.
